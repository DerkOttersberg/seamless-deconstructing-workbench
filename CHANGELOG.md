# Changelog

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
