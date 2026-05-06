# kotlin-npm-tooling

This directory is used to manage npm dependencies required by Kotlin Gradle plugin
for building, testing, and running Kotlin JS and WasmJS projects.

The `package.json` file in this directory is used as the single-source-of-truth
for KGP's npm tooling dependencies.
A single directory is used to help tools (like Dependabot) upgrade the dependencies.

npm dependencies are not installed directly here.
Instead, Gradle tasks use `package.json` to generate lockfiles (which are bundled into KGP)
and Kotlin files for accessing the dependencies.

### Maintenance

The versions must be kept up to date periodically, or in case of security issues.

The versions can be modified manually.

After updating the dependencies, make sure to regenerate the lockfiles.
To regenerate the lockfiles, run

```shell
./gradlew :kotlin-gradle-plugin:updateKgpNpmToolingDependencies
```

#### Upgrade all versions

All versions can be updated to the latest compatible version:

```shell
./gradlew :kotlin-gradle-plugin:updateKgpNpmToolingDependencies --update-versions
```

Note: this uses `npm update`, which will respect any version ranges.
The main benefit is it's easier to use npm version ranges to ensure KGP uses a fixed version
(e.g. fix Webpack to 5.101.x because of
[KT-67435](https://youtrack.jetbrains.com/issue/KT-67435/K-Wasm-import.meta.url-transforming-into-absolute-local-path-in-webpack)
).
The downside is major versions must be updated manually.
See https://docs.npmjs.com/cli/v11/commands/npm-update#example for more details.
