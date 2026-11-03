# Compatibility with other mods

## What Velox touches

Velox changes how plain item entities (`ItemEntity`) move. To do that it hooks into a few places in the game: the item's tick, how entities are checked for collisions and block effects, the end of each level's tick, and block changes in loaded chunks. Another mod that changes the same code in the same way can clash with it.

## What is known

Nothing here has been tested with a list of other mods yet, so there is no "tested with" list. These are facts from the code, not test results.

| Case | What Velox does |
|---|---|
| A mod's item drops as a normal item entity | Velox handles it like any other item. |
| A mod uses its own subclass of the item entity | Velox leaves it to the normal game and to that mod. |
| A mod changes an item's gravity, or lets it ride something | Velox leaves it to the normal game. |
| A mod moves, pushes or teleports items | Velox notices the change and reads the item again on the next step. |
| Another mod changes the same game code as Velox | A clash is possible. If Velox throws an error, it switches itself off and the server keeps running (see [Troubleshooting](Troubleshooting)). |

## If you find a clash

1. Read the `Velox:` lines in the log. They name the packages of other mods that appear in the error.
2. Add that mod's items to `excludedItems` ([Settings](Settings)), or set `enabled = false`.
3. [Open an issue](https://github.com/bichal/Velox/issues) with the log, your Minecraft and NeoForge versions, and the mod list. Every report that says which mods work together, or do not, belongs on this page.
