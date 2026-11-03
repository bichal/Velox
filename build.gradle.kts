@file:Suppress("AvoidDuplicateDependencies")

import net.bichal.gradle.BichalModExtension

plugins {
    id("bichal.neoforge-mod")
    idea
}

// Celeris (dependency and Maven repositories), neoforge.mods.toml expansion,
// legal files and buildAndCollect come from bichal.neoforge-mod:
// [depends_on.celeris] and [legal] in the TOML.

val modProps = extensions.getByType(BichalModExtension::class.java).props
val mainSourceSet = sourceSets.main.get()
val runDir = rootProject.file("versions/${modProps.currentVersion}/run")
val parityDir = rootProject.file("versions/${modProps.currentVersion}/run-parity")
val parityJavaDir = rootProject.file("versions/${modProps.currentVersion}/run-parity-java")

neoForge {
    runs {
        all {
            sourceSet = mainSourceSet
        }

        register("client") {
            client()
            gameDirectory = runDir
            systemProperty("neoforge.enabledGameTestNamespaces", modProps.modId)
        }

        register("server") {
            server()
            gameDirectory = runDir
            programArgument("--nogui")
            systemProperty("neoforge.enabledGameTestNamespaces", modProps.modId)
        }

        // Velox against vanilla, bit for bit, on a headless server: ParityHarness.
        register("parity") {
            server()
            gameDirectory = parityDir
            programArgument("--nogui")
            systemProperty("velox.parity", "true")
            // CI checks the native kernel too: it is opt-in for players, so ask for it here.
            systemProperty("velox.nativeKernel", "true")
        }

        // The same check on Celeris's Java kernel: what players get by default.
        register("parityJava") {
            server()
            gameDirectory = parityJavaDir
            programArgument("--nogui")
            systemProperty("velox.parity", "true")
            systemProperty("celeris.physics.engine", "java")
        }
    }
}

listOf(runDir, parityDir, parityJavaDir).forEach { if (!it.exists()) it.mkdirs() }

// A fresh flat world for every check. The EULA is accepted for this throwaway
// test server only; each Minecraft version gets its own port (they may run in parallel).
val parityPort = 25600 + (modProps.currentVersion.hashCode() and 0x1ff)

fun registerPrepare(taskName: String, runDirectory: File, serverPort: Int) = tasks.register(taskName) {
    val dir = runDirectory
    val port = serverPort
    doLast {
        dir.resolve("world").deleteRecursively()
        dir.resolve("velox-parity.txt").delete()
        dir.resolve("eula.txt").writeText("eula=true\n")
        // A small batch capacity, so that the check can throw in more items than the batch holds.
        dir.resolve("config").mkdirs()
        dir.resolve("config/velox-common.toml").writeText("maxItemsPerLevel = 1024\n")
        dir.resolve("server.properties").writeText(
            listOf(
                "level-type=minecraft\\:flat",
                "generate-structures=false",
                "spawn-monsters=false",
                "online-mode=false",
                "server-port=$port",
                "max-tick-time=-1",
                "view-distance=4",
                "simulation-distance=4",
            ).joinToString("\n", postfix = "\n")
        )
    }
}

val prepareParity = registerPrepare("prepareParity", parityDir, parityPort)
// A second range of ports, so the two checks never collide with each other or with another version's.
val prepareParityJava = registerPrepare("prepareParityJava", parityJavaDir, 26112 + (modProps.currentVersion.hashCode() and 0x1ff))

tasks.matching { it.name == "runParity" }.configureEach { dependsOn(prepareParity) }
tasks.matching { it.name == "runParityJava" }.configureEach {
    dependsOn(prepareParityJava)
    mustRunAfter("runParity")
}

tasks.register("parity") {
    group = "verification"
    description = "Runs Velox and vanilla side by side on a headless server and compares every item, every tick, with Celeris's native kernel and with its Java kernel."
    dependsOn("runParity", "runParityJava")
    val checks = listOf(
        Triple("native kernel", parityDir, true),
        Triple("Java kernel", parityJavaDir, false),
    )
    doLast {
        val kernelLine = Regex("""Velox: items simulated by Celeris \(([^)]+)\).*""")
        for ((label, dir, wantNative) in checks) {
            val report = dir.resolve("velox-parity.txt")
            val serverLog = dir.resolve("logs/latest.log")
            if (!report.isFile) {
                throw GradleException("Velox parity ($label): no report in $report (the server stopped before the check ended)")
            }
            val text = report.readText()
            println("== Parity, $label ==")
            println(text)
            if (!text.lineSequence().first().endsWith("PASS")) {
                throw GradleException("Velox parity failed on the $label: see $report")
            }
            val kernel = if (serverLog.isFile) serverLog.useLines { lines -> lines.firstNotNullOfOrNull { kernelLine.find(it) } } else null
            println("Kernel ($label): ${kernel?.value ?: "not logged"}")
            val engine = kernel?.groupValues?.get(1)
            if (wantNative) {
                // With the right JVM flags players can get Celeris's native kernel, so CI checks that one: a
                // fall back to the Java kernel (JVM flags, a library that did not load) must not pass
                // unnoticed. Locally it is only reported.
                if (System.getenv("CI") == "true" && engine?.startsWith("native") != true) {
                    throw GradleException("Velox parity ran without Celeris's native kernel: see $serverLog")
                }
            } else if (engine != "java") {
                throw GradleException("Velox parity was meant to run on the Java kernel but ran on ${engine ?: "no logged kernel"}: see $serverLog")
            }
        }
    }
}
