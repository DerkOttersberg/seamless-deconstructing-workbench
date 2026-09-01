# Seamless Deconstructing Workbench

Seamless Deconstructing Workbench adds a salvage workbench that resolves
shaped crafting recipes and returns their ingredients with configurable loss
and durability scaling. Version `2.1.0+mc26.2` supports Minecraft Java 26.2
on Fabric, Forge, and NeoForge and requires Seamless API 2.x.

The registry namespace remains `seamlessdeconstructor`, including the
`reverse_deconstructor` block, item, block entity, and menu IDs, so copied
worlds can retain existing workbenches.

## Architecture

- `common` contains recipe selection, fractional output, inventory rules,
  block entity/menu/screen behavior, rendering, resources, and tests.
- `fabric`, `forge`, and `neoforge` contain registration, configuration-path,
  creative-tab, screen, and renderer lifecycle adapters.
- Architectury Loom is build tooling only; Architectury API is not required at
  runtime.
- External deconstruction registrations and modifiers are consumed through
  Seamless API. The API is a normal dependency and is not shaded into this mod.

Completed salvage operations are planned atomically. Their exact randomized
results and component-bearing input identity are saved while an output is
blocked, so freeing capacity or reloading the world cannot reroll the result.
Enchanted inputs also require an unmodified book and produce the exact stored
enchantments as part of the same transaction.

Automation is exposed through Fabric Transfer API, Forge item capabilities,
NeoForge item capabilities, and the shared sided-container rules. The top and
sides accept input and one unmodified book; the bottom exposes only outputs.

The old `seamlessdeconstructor.json` configuration file is copied to
`seamless-deconstructing-workbench.json` on first launch. Both the original
file and a `.bak` copy are retained. Invalid canonical files are retained as
`.invalid.bak` before sanitized defaults are written atomically.

## Build

Use Java 25 and run:

```text
gradlew.bat clean check build
```

The build uses the sibling `seamless-api` checkout as a Gradle composite and
produces one jar per loader under each loader module's `build/libs` directory.
`check` runs unit tests and isolated Fabric, Forge, and NeoForge GameTest
servers. The gameplay suite covers preserved IDs, live processing, exact
enchantment-book output, atomic blocked/reloaded operations, sided automation,
loader-native storage adapters, and menu shift-click routing. Every loader run
also enforces a discovered-test count so an empty GameTest launch cannot pass.

See [PORTING.md](PORTING.md) before changing Minecraft or loader versions and
[MIGRATION.md](MIGRATION.md) before upgrading copied worlds or configurations.
