# Changelog

## 2.1.0+mc26.2

- Made deconstruction commits atomic: exact randomized results are rolled once,
  persisted with the component-bearing input identity, and never dropped or
  rerolled while output space is blocked or a world is reloaded.
- Made enchantment extraction part of the same transaction. Only an unmodified
  book is accepted, and the exact enchantment set is preserved on the output.
- Added component-aware merging and actual per-item maximum stack sizes to
  output planning.
- Added synchronized processing and blocked reasons to the menu and screen,
  with corrected shift-click routing for input, books, and outputs.
- Added Fabric Transfer API, Forge item capability, and NeoForge item capability
  adapters while preserving the shared sided automation rules.
- Hardened legacy configuration migration, invalid-file backups, failure
  logging, sanitization, and atomic canonical writes.
- Added focused atomic-planner/NBT unit coverage and gameplay GameTests for all
  three loaders, including loader-native automation assertions and discovery
  count guards.
- Pinned the build to Seamless API `2.0.1+mc26.2`, retained runtime compatibility
  with Seamless API 2.x, and constrained Fabric API metadata to the built-against
  minimum instead of a wildcard.

## 2.0.0+mc26.2

- Ported to Minecraft Java 26.2 and Java 25 on Fabric, Forge, and NeoForge.
- Unified recipe resolution, loss/durability rules, block entity, inventory,
  menu, screen, renderer, and resources in a shared `common` module.
- Preserved the historical `seamlessdeconstructor:reverse_deconstructor`
  registry IDs for copied-world compatibility.
- Added Seamless API 2.x registrations and output modifiers without shading
  the API.
- Added canonical configuration migration while retaining the old file and a
  backup.
- Added unit tests for selection, quantities, fractional output, and config
  migration plus a live Fabric processing GameTest.
