# Migration to 2.1.0 for Minecraft 26.2

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
- Machines saved mid-process continue from their historical `Items`, `Progress`,
  and `MaxProgress` fields. New blocked operations additionally save an exact
  `PendingOperation`; do not remove it manually, because it prevents an output
  reroll after reload.
- Enchanted inputs now require a completely unmodified book. Renamed books or
  books carrying any custom component are deliberately rejected and are never
  consumed as enchantment carriers.
- Hopper-style automation keeps the historical sided layout. Loader-native
  Fabric, Forge, and NeoForge storage integrations now enforce the same rules.

Back up a world and configuration directory before upgrading. Validate only a
copy first, including machine inventories, active processing, and save/reload.
