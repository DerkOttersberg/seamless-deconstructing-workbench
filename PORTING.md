# Porting Seamless Deconstructing Workbench

Minecraft, Java, loader, and build-tool versions live only in
`gradle/libs.versions.toml`. Update that catalog first and compile `common`
against Minecraft's official names before adapting loader entrypoints.

## Stable common behavior

- Keep `seamlessdeconstructor:reverse_deconstructor` for the block, item, block
  entity, and menu. Changing these IDs loses placed blocks or installation
  continuity.
- `DeconstructionResolver` owns deterministic shaped-recipe selection and
  Seamless API registrations.
- `DeconstructionPlan`, `FractionalOutputRoller`, and
  `DeconstructionOutputRules` own quantity/loss/durability behavior.
- The common block entity owns processing and inventory conservation. Loader
  modules only register objects, screens, renderer hooks, and config paths.
- `PlatformServices` is passed explicitly; do not introduce reflection or
  `ServiceLoader` discovery.

Do not add loader imports to `common`; `verifyCommonIsolation` rejects them.
Seamless API remains an external 2.x dependency and must not be shaded.

## Port checklist

1. Update `gradle/libs.versions.toml` and resource pack/data pack metadata.
2. Run `gradlew.bat clean check build` using the target Java version.
3. Inspect all jars for loader-metadata isolation and canonical filenames.
4. Boot dedicated servers and clients for all three loaders.
5. Run the live processing GameTest and check recipe resolution, fractional
   output, enchanted-item handling, automation faces, and menu closure.
6. Upgrade only copied historical worlds/configs and confirm the registered
   workbench survives with its inventory and block entity intact.
7. Run the matching four-mod combined profiles and retain logs/screenshots.
