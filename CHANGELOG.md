# Changelog

## 2.1.1+mc1.21.1 (unreleased)

- Port code/resources to Minecraft 1.21.1 / Java 21 on Fabric, Forge and NeoForge.
- Bundle current CurseForge icons with provenance and all-loader artifact checks.
- Keep settings labels/tooltips sharp: avoid 1.21.1's redundant base-screen blur pass.
- Preserve IDs, licenses, server authority and exact item components.
- Build/GameTest gates pass; real runtime acceptance remains in progress.

## 2.1.1+mc1.20.1

- Backport the current shared gameplay architecture to Minecraft 1.20.1, Java 17,
  Fabric and Forge. NeoForge is intentionally excluded from this line.
- Restore remapped loader jars, mixin refmaps, legacy NBT/data formats and bounded
  networking without changing public compatibility or registry namespaces.
- Preserve current config migration, UI clarity and item-conservation safeguards.
- Clamp Fabric book-slot insertion before committing a transfer transaction.


## 2.1.1+mc26.3

- Adapt block predicates, block APIs, and pose rotations. Preserve registry IDs, fractional output rules, config migration, and the empty book-slot tooltip. Isolate Forge development GameTests from production jars.
- Minecraft 26.3 only, Java 25; Fabric, Forge, and NeoForge.
- Forge 66.0.9 and NeoForge 26.3.0.48-beta are upstream beta loaders.
- Existing 26.2 releases remain separate; no blanket 26.* compatibility.

## 2.1.0+mc26.2

- Added a dedicated Mods-menu icon on all loaders.
- Rebuilt settings with responsive pages, visible ARGB labels, seconds and
  percentage units, full explanations, and named validation errors.
- Added an empty-book-slot tooltip explaining plain books, enchantment
  preservation, consumption, and when books are optional; added input-slot help.
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
