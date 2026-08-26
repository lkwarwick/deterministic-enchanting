# Deterministic Enchanting

Deterministic Enchanting replaces the random enchantment-table offers with a
deterministic menu of compatible enchantments and levels.

## Releasing a new mod update

This is the normal process for adding a feature or fixing a bug while keeping
backwards compatibility with the Minecraft versions you support.

Keep one branch for each supported Minecraft line. For example:

```text
main       Minecraft 26.2
mc/26.1    Minecraft 26.1
```

When adding a feature, make it on `main`, run the build, and open a pull
request. After it is merged, apply the same change to every maintained older
branch. Use a pull request or cherry-pick the feature commit, then build and
test each branch separately. If the Minecraft APIs differ, adapt the change
for that branch instead of forcing one implementation onto every version.

For each supported branch:

1. Update `version` in `gradle.properties` to the release version.
2. Run the build:

	```text
	./gradlew --no-daemon build
	```

3. Commit and push the branch, then wait for CI to pass.
4. Create and push a tag containing both the mod version and Minecraft version:

	```text
	git tag v1.1.0-mc26.2
	git push origin v1.1.0-mc26.2
	```

On the older branch, use the matching tag:

```text
git tag v1.1.0-mc26.1
git push origin v1.1.0-mc26.1
```

Each tag starts the **Release to Modrinth** workflow. It checks that the tag’s
Minecraft version matches the branch, builds that branch, and uploads one
remapped JAR. Modrinth receives separate versions for each Minecraft line.
You do not upload JARs manually.

Use a patch version for a bug fix, such as `1.0.1`, and a minor version for a
new feature, such as `1.1.0`.

When a new Minecraft version is released, create a new branch from the current
code, update Minecraft, Fabric Loader, Fabric API, and any affected code, then
test it independently. Keep the older branch active for backwards-compatible
fixes and publish both tags when a feature or fix supports both versions.

## Local development

For the existing project checkout, use Java 25. The distributable JAR from a
local build is written to `build/libs/`.

## License

This project is available under the CC0 license.
