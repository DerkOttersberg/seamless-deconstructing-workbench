# Migration to 2.0.0 for Minecraft 26.2

- Install the jar matching the active loader and install Seamless API 2.x as a
  separate dependency.
- The namespace remains `seamlessdeconstructor`.
- The `reverse_deconstructor` block, item, block entity, and menu IDs are
  preserved so copied worlds can retain existing machines.
- On first launch, `seamlessdeconstructor.json` is copied to
  `seamless-deconstructing-workbench.json`. The original file remains and a
  `.bak` backup is retained.
- Recipe resolution now shares one implementation across Fabric, Forge, and
  NeoForge and also consumes Seamless API deconstruction registrations.

Back up a world and configuration directory before upgrading. Validate only a
copy first, including machine inventories, active processing, and save/reload.
