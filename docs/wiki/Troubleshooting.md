# Troubleshooting

Start with the log. Velox writes lines when it starts, and each line says what it is doing. Search the log for lines that start with `Velox`.

## What the log lines mean

| Log line | What it means | What to do |
|---|---|---|
| `Velox: disabled in config -- items use vanilla physics` | `enabled` is `false`. | Set `enabled = true`, and restart the server. |
| `Velox: Celeris physics engine unavailable (why) -- items use vanilla physics` | Celeris could not start its physics engine. The text in the brackets says why. | Read the Celeris lines in the log. See the [Celeris troubleshooting page](https://github.com/bichal/Celeris/wiki/Troubleshooting). |
| `Velox: items simulated by Celeris (java)` | Velox works, with the Java kernel. This is the default. | Nothing. The server tick is the same as with the native kernel in our tests. To try the native kernel, add `-Dvelox.nativeKernel=true`. See [Requirements and files](Requirements-and-files). |
| `Velox: items simulated by Celeris (native-jni-avx2)` | Velox works, with the native kernel (you asked for it with `-Dvelox.nativeKernel=true`). The name in the brackets can differ on your computer. | Nothing. |
| `Velox: item physics for minecraft:overworld on java (capacity 32768)` | Velox started handling that world. The capacity is the most items it handles there. | Nothing. |
| `Velox: switched OFF after an error in ...` | Velox hit an error, most likely a clash with another mod, and switched itself off. The server keeps running with the normal game physics. | See [Velox switched itself off](#velox-switched-itself-off) below. |

## Common problems

### Some items act like vanilla

This is expected for some items. Velox leaves these to the normal game: items in water or lava, on hoppers, slabs, stairs or carpets, and more. See [How it works](How-it-works).

Items above `maxItemsPerLevel`, and items outside `windowSections`, also keep the normal game physics. You can raise these in [Settings](Settings).

### I changed a setting, and nothing changed

Velox reads its settings when the server starts. Restart the server after you change them.

### The server still lags with many items

Velox removes most of the physics work, but Minecraft still does other work for each item. Past about 25,000 to 35,000 items, even Velox cannot keep 20 ticks per second. Fewer items always helps most. See [Benchmarks](Benchmarks) for the numbers.

### My Minecraft version is not in the list

Velox is only supported on the versions listed in [Requirements and files](Requirements-and-files). Other versions are not tested, so they are not supported.

### Velox says Celeris is missing

Velox needs Celeris 0.3.1.1 or newer, on the server. Without it NeoForge stops the server while it loads mods, with a missing-dependency error, so Velox never gets to write a log line. Install Celeris, then restart the server.

### Velox switched itself off

If anything in Velox throws an error, Velox does not stop the server. It switches itself off for the rest of the session, gives every item back to the normal game, and writes lines that start with `Velox:` to the log, with the full error. After a restart it tries again.

1. Find the line `Velox: switched OFF after an error in ...`. The next lines list the packages of other mods that appear in the error. One of them may clash with Velox.
2. If the clash comes from one mod's dropped items, add that mod to `excludedItems` (see [Settings](Settings)), for example `somemod:*`.
3. If it keeps happening, [open an issue](https://github.com/bichal/Velox/issues) with that part of the log. Until it is fixed you can set `enabled = false`.

### How do I see what Velox is doing?

Run `/velox status`. Operators can use it in the game and anyone can use it from the console. It shows whether Velox is running, which Celeris kernel it uses, and for each dimension how many items it handles, how many took the batch result in the last tick, and how long the step took. After an error it shows that Velox switched itself off, and why. Include its output when you report a problem.

## Frequently asked questions

**Do players need Velox?**
No. Velox runs on the server only.

**Does Velox change how items behave?**
No. Items end up in the same places as in the normal game. Velox only changes how fast the work is done.

**Does Velox affect items from other mods?**
Only plain item entities are handled. Items from other mods that are a different kind of entity are left alone.

**Where do I report a bug?**
On the [issues page](https://github.com/bichal/Velox/issues). Include the Velox lines from the log.
