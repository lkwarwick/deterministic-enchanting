# Deterministic Enchanting Repository Instructions

## Project Purpose

This is a Fabric Minecraft mod named Deterministic Enchanting. Its goal is to replace the vanilla enchantment-table UI with a deterministic interface that exposes every compatible enchantment and every supported level for the current item. The UI should show bookshelf availability, whether the item already has an enchantment, and whether the player can afford the option. Players should select a specific enchantment and level directly instead of choosing among three random vanilla offers.

The implementation is incomplete. The current handler mixin is diagnostic code that scans possible powers and logs discovered enchantment-level pairs; it does not yet change vanilla behavior or provide a custom UI.

## Project Structure

- `src/main/java`: common/server-side Java code and common mixins.
- `src/client/java`: client-only Java code and client mixins.
- `src/main/resources`: common mixin configuration and `fabric.mod.json`.
- `src/client/resources`: client-only mixin configuration.
- `docs`: design and implementation notes, including the deterministic enchanting roadmap.
- `run`: local Minecraft run directory and test-world data; do not treat generated world data as source code.
- `build`: generated Gradle/Loom output; do not edit it manually.

The Gradle build uses split environment source sets. Keep Minecraft/server-safe logic in `src/main/java`; keep screen rendering and other client-only references in `src/client/java`.

## Versions and Build

- Minecraft: `26.2`
- Fabric Loader: `0.19.3`
- Fabric API: `0.158.0+26.2`
- Fabric Loom: `1.17-SNAPSHOT` in project properties; the resolved build currently reports Loom `1.17.19`.
- Java source and target release: `25`
- Mod ID: `deterministic-enchanting`
- Maven group: `lkwarwick.deterministicenchanting`
- Mod version: `1.0.0`

Use the Gradle wrapper from the repository root. Useful commands are:

- `./gradlew --no-daemon compileJava compileClientJava`
- `./gradlew --no-daemon test`
- `./gradlew --no-daemon runClient`
- `./gradlew --no-daemon build`

## Current Implementation Anchors

The primary current file is `src/main/java/lkwarwick/deterministicenchanting/mixin/EnchantmentScreenHandlerMixin.java`. It injects at the head of `EnchantmentMenu.getEnchantmentList`, scans powers 1 through 30, calls `EnchantmentHelper.getAvailableEnchantmentResults`, and logs the first power found for each enchantment-level pair.

The common mixin configuration is `src/main/resources/deterministic-enchanting.mixins.json`. The client configuration is `src/client/resources/deterministic-enchanting.client.mixins.json`; it currently contains only the example client mixin. The client initializer is `src/client/java/lkwarwick/deterministicenchanting/client/DeterministicEnchantingClient.java`.

## Implementation Rules

1. Keep offer generation and final enchantment validation server-authoritative.
2. Do not identify enchantments with localized display strings. Use registry identifiers or holders.
3. Remember that vanilla enchantment menus and screens assume three indexed offers. A larger return list from `getEnchantmentList` alone will not implement the requested UI.
4. Revalidate item state, compatibility, bookshelf power, player experience, lapis cost, and existing-enchantment rules when the server receives a selection.
5. Keep the catalog builder independent from rendering so it can be tested without starting the client.
6. Preserve the existing source-set boundary and avoid importing client-only classes into common code.
7. Prefer the Minecraft and Fabric APIs already used by the project over new abstractions or dependencies.
8. Make focused changes and run a narrow compile or test command after each implementation slice.

The recommended implementation sequence is documented in `docs/deterministic-enchanting-roadmap.md`.
