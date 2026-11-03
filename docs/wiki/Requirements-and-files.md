# Requirements and files

## What you need

- A NeoForge server for Minecraft 1.21 to 1.21.11, or 26.1 to 26.3.
- [Celeris](https://github.com/bichal/Celeris) 0.3.1.1 or newer, on the same server. Velox does not work without it.
- Java 21 for Minecraft 1.21.x. Java 25 for Minecraft 26.x.

Players do not need to install Velox.

## Pick your file

| Minecraft version | File |
|---|---|
| 1.21 to 1.21.6 | `velox-0.1.1+1.21.1.jar` |
| 1.21.7 to 1.21.10 | `velox-0.1.1+1.21.10.jar` |
| 1.21.11 | `velox-0.1.1+1.21.11.jar` |
| 26.1 to 26.3 | `velox-0.1.1+26.1.jar` |

**Example:** Your server runs Minecraft 1.21.5. That is in the first row, so use `velox-0.1.1+1.21.1.jar`.

Every file was checked against vanilla on each version in its row. A test server ran Velox and vanilla side by side, and every item had to move exactly as it does in vanilla.

## Java flags

Velox works without flags, on Celeris's Java kernel. In our tests with 20,000 items the Java, JNI and FFM kernels tie on the server tick (see [Benchmarks](Benchmarks)), so you do not need native code. To ask Celeris for its native kernel anyway, add `-Dvelox.nativeKernel=true`; it uses JNI, which needs no other flag. If the native library cannot load, Celeris uses the Java kernel and Velox still works. To force the Java kernel even then, add `-Dceleris.physics.engine=java`.

Only the FFM binding needs a flag, and Celeris tries it only if JNI cannot load (and only when you asked for the native kernel):

| Minecraft | Java | Flag for FFM |
|---|---|---|
| 1.21.x | 21 | `--enable-preview` |
| 26.x | 25 | None |

Do not add `--enable-native-access`. On Java 21 it blocks FFM calls from NeoForge mods.

To see which kernel is running, search the log for `Velox: items simulated by Celeris`. The text in the brackets tells you. See [Troubleshooting](Troubleshooting).
