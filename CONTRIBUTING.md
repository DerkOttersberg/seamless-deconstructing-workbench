# Contributing

Use Java 25. Put recipe, quantity, inventory, block-entity, menu, and screen
behavior in `common`; keep loader modules limited to registration and lifecycle
adapters.

Before submitting a change, run:

```text
gradlew.bat clean check build
```

Add tests when changing recipe selection, fractional rolls, loss, durability,
enchantment extraction, configuration migration, or inventory rules. Never
change compatibility registry IDs without an explicit world migration and
documentation in `MIGRATION.md` and `CHANGELOG.md`. Test upgrades only on
copied worlds.
