☑️ Replace the NPM override DSL with plain version strings

Contributors: Adam Semenenko

Status: Draft

Deadline: <TBD>

Shared Externally: No

Reviewer
Status
Yahor Berdnikau
Not started
<insert people here who should approve this proposal>

Note: checkmarks ☑️ ✅ are used to remember which chapters are "good enough", when editing. They also make low-level headings somewhat easier
to see.

Glossary

- **override**, an entry in `package.json` `overrides` telling npm to force a specific version/range for a (possibly transitive)
  dependency.
- **node-semver**, the version/range grammar npm accepts (`^1.0.0`, `>=1.0.0 <2.0.0`, `1.x`, `a - b`, `x || y`, …).
  See https://github.com/npm/node-semver#versions
- **NDOC**, Gradle `NamedDomainObjectContainer`, the lazy, name-keyed collection type used throughout KGP.

☑️ What is this proposal solving?

KGP lets users pin/restrict the versions of transitive npm dependencies (written to `package.json` `overrides`).
Currently, user can do this with a bespoke include/exclude DSL:

```kotlin
rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension>().apply {
        override("lodash") {
            include("^1.0.0")
            exclude("~1.2.1", "1.3.0 - 1.4.0")
        }
        override("react", "16.0.0")
    }
}
```

Behind that DSL sits a large, hand-written semver algebra, `NpmOverride`, `NpmRange`, `NpmRangeVisitor`, `buildNpmVersion`,
and a custom `SemVer` class.
The DSL parses each include/exclude fragment into `NpmRange` objects, intersects/inverts/unions them,
and re-serializes the result back into a single node-semver range string that is written verbatim into `package.json`.

The DSL is used by KGP users to control KGP's dev npm dependencies.

## Problems

The custom DSL is not documented and has no KDoc.

Because KGP JS/WasmJS does not follow Gradle best practices KT-87704 there are no generated Kotlin DSL accessors. Users must use verbose
syntax like `rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java)`.

Users need to be able to control version overrides for the 'root' npm build. It's not clear how to support this in a Isolated Projects
compatible way.

The `SemVer` class is located in the `js` package. It is only used internally, but is (unintentionally) part of the KGP public API.
Despite being intended for npm SemVer parsing it is (mis)used in non-JS parts of KGP for non-JS work.

npm already accepts node-semver range strings directly in `overrides`.
The include/exclude DSL is therefore a re-implementation of node-semver range syntax on top of node-semver range syntax: users must learn a
Kotlin abstraction that exists only to produce a string the
ecosystem understands natively.
It is more code to maintain, only has basic tests, throws errors during configuration time for invalid input, and is harder to document
than "put the range string node-semver already documents."

## Outcome

Outcome we want: users declare the range string directly. KGP adds it to `package.json` untouched.

## Assumptions

- **KGP Yarn is already deprecated.** KT-84662
  The parallel Yarn `resolution(...)` DSL is being deprecated as part of that separate Yarn-deprecation effort, not by this proposal.
- **Consequently, the npm override path becomes the last consumer of the shared semver machinery** (`NpmRange` / `NpmRangeVisitor` /
  `buildNpmVersion` / custom `SemVer`). Once the npm path stops using it, that machinery has no live callers, so `SemVer` and all the npm
  range code can be deprecated in a later PR.

### Anti-Goals

- *Not building a new Yarn DSL.* Yarn is being deprecated separately KT-84662. No throwaway effort is spent adding a Yarn equivalent of the
  new string DSL.
- *Not changing what gets written to `package.json`*. (`overrides` field, same semantics).
- *Not validating range-string syntax inside KGP*. Errors from npm regarding invalid versions are JSON-escaped and written to `package.json`
  directly.
- *Not supporting npm nested `overrides` objects.* npm accepts a recursive value (`string | { ".": string, "<child>": value, … }`, plus
  `$ref` values and versioned keys like `foo@1.x`) to scope an override to a dependency's subtree. Each entry here maps to a single flat
  range string only. This matches the old DSL (never supported nesting, no regression). Users needing nested overrides use
  [the existing `package.json` customisation](https://kotlinlang.org/docs/js-project-setup.html#package-json-customization).

## Solution (overview)

Replace the npm include/exclude DSL with a Gradle `NamedDomainObjectContainer` keyed by package name.
Each entry carries a single lazy `range: Property<String>` holding a node-semver range string.
Each entry is added to `overrides` in `package.json`.

KGP must JSON-escape declared names and versions to ensure the produced JSON is valid.

New shape:

```kotlin
rootProject.plugins.withType(org.jetbrains.kotlin.gradle.targets.js.nodejs.NodeJsRootPlugin::class.java) {
    rootProject.the<org.jetbrains.kotlin.gradle.targets.js.npm.NpmExtension>().apply {
        overrides // deprecated
        versionOverrides.register("lodash") {
            range.set(">=1.0.0 <1.2.1 || >1.4.0 <2.0.0")
        }
        versionOverrides.register("react") {
            range.set("16.0.0")
        }
    }
}
```

The old npm `override(...)` DSL **and** the now-orphaned semver algebra (`NpmOverride`, `NpmRange`, `NpmRangeVisitor`, `buildNpmVersion`)
are marked `@Deprecated` and scheduled for removal.

`SemVer` will be deprecated in a later change, and usages updated to use `com.github.gundy.semver4j` instead.

☑️ Solution

### New public DSL surface

- `BaseNpmExtension.versionOverrides: NamedDomainObjectContainer<NpmOverrideSpec>`, reached via `the<NpmExtension>()` / `npm {}`.
- Spec type (`Named` by package name, one string property):

  ```kotlin
  abstract class NpmOverrideSpec @Inject internal constructor(
      private val packageName: String,
  ) : Named {
      abstract val range: Property<String>
      override fun getName(): String = packageName
  }
  ```

The name `versionOverrides` is deliberately distinct from the deprecated `overrides` so the two can coexist during the removal window (see
FAQ).

Key files:

- `.../js/npm/NpmOverride.kt`, `NpmOverrideSpec`
- `.../js/npm/BaseNpmExtension.kt`, `versionOverrides` declaration + `produceEnv()`
- `.../js/npm/NpmExtension.kt` / `WasmNpmExtension.kt`, thread the `ProviderFactory` needed to wire the container

### Pass-through data flow (the range string is never parsed)

`versionOverrides` → copied into `NpmEnv.newOverrides` in `BaseNpmExtension.produceEnv()` → `NpmEnvironment.newOverrides` →
`Npm.prepareRootProject` maps it to `Map<pkg, range>` → `rootPackageJson.overrides`.

```kotlin
// Npm.kt
packageManagerEnvironment.newOverrides
    ?.associate { it.name to it.range.get() }
    .orEmpty()
```

Consequence in `PackageJson.kt`: the old `chooseVersion(...)` range-intersection logic (which could throw "does not intersect") is removed;
when two versions collide the values are simply space-joined (`listOfNotNull(oldVersion, newVersion).joinToString(" ")`), letting the
package manager resolve it.

### Deprecations (removal in Kotlin 2.7)

Mark `@Deprecated(..., level = WARNING)` with a `ReplaceWith` where feasible and a "Scheduled for removal in Kotlin 2.7" message:

- npm DSL entry points: `NpmOverride` (incl. `include`/`exclude`/`toVersionString`), `BaseNpmExtension.overrides`,
  `override(path, configure)`, `override(path, version)`.
- The orphaned semver algebra (no live callers once the npm path stops using it, see Assumptions): `NpmRange` + its operators
  (`union`/`invert`/`intersect`/`hasIntersection`/`maxStart`/`minStart`/`maxEnd`/`minEnd`), `NpmRangeVisitor`, `versionToNpmRanges`,
  `includedRange`, `buildNpmVersion` (`NpmVersions.kt`).
- The custom `SemVer` class + `fromGradleRichVersion`/`toVersion`/`toSemVer`/`min`/`max` (`semver.kt`), and `GradleNodeModule.semver`.

### semver4j migration (the one still-needed semver consumer)

After the override path stops parsing versions, the only remaining semver use is imported-package dedup. Migrate it off the custom `SemVer`
to `com.github.gundy.semver4j`'s `Version`:

- New `internal fun createVersionFromGradleRichVersion(version, loose = false): com.github.gundy.semver4j.model.Version` in `semver.kt`,
  reimplementing the Gradle rich-version → semver mapping.
- `GradleNodeModule.parsedVersion: Version` (replaces `.semver`), used by `NpmImportedPackagesVersionResolver` sort.
- `kotlinDomApiDependencyManagement.kt`, `kotlinTestDependencyManagement.kt`, `stdlibDependencyManagement.kt`,
  `GenerateSyntheticLinkageImportProject.kt` switch `SemVer(...)` → `Version.fromString(...)` / `createVersionFromGradleRichVersion(...)`.
- `build.gradle.kts`: add `com.github.gundy:semver4j:0.16.4:nodeps`.

### Plumbing

`NpmExtension` / `WasmNpmExtension` gain a `ProviderFactory` parameter so the new container is wired lazily
(`addAllLater(providers.provider { ... })`).

### Public `.api` impact

`libraries/tools/kotlin-gradle-plugin/api/all/kotlin-gradle-plugin.api` must be regenerated. Expected: `+getOverrides2`, a new
`NpmOverrideSpec` entry; the `NpmEnv`/`NpmEnvironment` value objects lose generated `data class` members (converting to plain classes with
hand-rolled deprecated `componentN`/`copy` for compat) and change `getOverrides` return type from `List` to `NamedDomainObjectContainer`.

### Remaining work to land the prototype

The working tree of `rr/adam.semenenko/js/KT-87489-update-npm-version-kdoc` is a WIP prototype that already implements most of this. To
finalize and align it with the npm-only scope:

1. **Drop the Yarn `resolutions2` additions**, the new Yarn container/spec and its wiring (`YarnResolutions.kt` `YarnResolutionSpec`,
   `BaseYarnRootExtension.resolutions2`, `YarnRootExtension.kt`, `BaseYarnRootEnvSpec.kt`, `YarnPluginApplier.kt`, `YarnEnv`/
   `YarnEnvironment`
   new fields, the Yarn assertions in `kotlin-js-npm-overrides`). No new Yarn DSL (see Anti-Goals). Deprecating the *existing* Yarn
   `resolution(...)` DSL belongs to the separate Yarn-deprecation effort, not this MR.
2. **Keep the semver deprecations + semver4j migration**, they are in scope under the stated assumption.
3. Remove transitional commented-out blocks (`Npm.kt`, `BaseNpmExtension.kt`, `PackageJson.kt`,
   `KotlinCompilationNpmResolution.disambiguateDependencies`, test-project comments).
4. KDoc `versionOverrides` pointing at node-semver syntax (complements committed KDoc in `dc0c228`).
5. Regenerate `.api` and review the diff.

☑️ Related work

- `dc0c228` "[Gradle] Improve KDoc for NPM dependency methods" (already committed), points users at node-semver syntax; complementary.
- The planned deprecation of Yarn support in KGP, a hard prerequisite for this proposal (see Assumptions).
- KT-87489, the tracking issue for this branch.

☑️ Prior Art

- **Keep the include/exclude DSL**, rejected: a redundant re-implementation of node-semver range syntax, more code, a bug source, harder to
  document.
- **`MapProperty<String, String>`** (`overrides.put("react", "16.0.0")`), the simplest "just strings" form. Rejected in favor of
  `NamedDomainObjectContainer`, which matches Gradle conventions used elsewhere in KGP, is lazily configurable, and leaves room to add
  per-package fields later without another breaking change.
- **Function overload** `override("react", "16.0.0")`, keeps imperative style but doesn't fit lazy configuration as cleanly. Rejected.
- **Do nothing**, users keep paying the abstraction tax and hitting the intersection-throws bug; maintenance burden of the semver algebra
  stays.

☑️ FAQ

Q: Range strings are no longer validated at configuration time, is that a regression?
A: The old DSL "validated" only by attempting to build a range, and could throw on non-intersecting inputs. node is the authoritative
validator of range syntax and reports clear errors at install time. Pushing validation to the tool that owns the format is simpler and more
correct.

Q: Why `versionOverrides` and not just `overrides`?
A: The existing `overrides` (a `ListProperty<NpmOverride>`) is deprecated but kept alive through the 2.7 removal window, so its name is
taken. A distinct name lets the new string container coexist without a source-breaking type change on the old one. `versionOverrides` also
reads well at the call site, `versionOverrides.register("react") { range.set("16.0.0") }`, and states plainly that it pins a version.

Q: What happens to two colliding versions of the same transitive package?
A: Previously KGP intersected ranges and could fail the build; now the values are space-joined and left to the package manager to
resolve.

Q: npm `overrides` support nested/scoped objects. How do I express those?
A: Out of scope for this DSL (see Anti-Goals). Use KGP's existing `package.json` customization: the `packageJson { }` block on a JS
compilation (`KotlinJsCompilation.packageJson(Action<PackageJson>)`) exposes `customField(key, value: Any?)`, whose value accepts an
arbitrary nested `Map`/`List` and is serialized as-is. Example:
`packageJson { customField("overrides", mapOf("bar" to mapOf("." to "2.0.0", "baz" to "3.0.0"))) }`. This keeps the common "pin a package"
case a one-liner while leaving full-fidelity npm overrides to the raw editing mechanism KGP already ships.

Q: What if Yarn support is *not* deprecated first?
A: Then the semver machinery still has a live consumer (Yarn's `resolution(...)` → `buildNpmVersion`) and cannot be deprecated yet. The
Assumptions section makes the sequencing explicit; if it does not hold, split this proposal into "add npm `versionOverrides` + deprecate npm
`override(...)`" now and defer the `NpmRange`/`SemVer` deprecation until Yarn is gone.

☑️ Errata



---

## Verification (implementation checklist)

- Integration test `Kotlin2JsGradlePluginIT.testNpmOverrides` (`@DisplayName("npm overrides works")`) runs the `kotlin-js-npm-overrides`
  test project with `yarn = false`, executes `jsPackageJson`/`rootPackageJson`/`kotlinNpmInstall`, and asserts `package.json`
  `overrides["lodash"] == ">=1.0.0 <1.2.1 || >1.4.0 <2.0.0"` and `overrides["react"] == "16.0.0"`, verbatim pass-through.
    - Run: `./gradlew :kotlin-gradle-plugin-integration-tests:test --tests "*Kotlin2JsGradlePluginIT*testNpmOverrides" -q`
- Unit tests `NpmRangeTest` / `SemVerTest` cover the now-deprecated machinery; add `@file:Suppress("DEPRECATION")` so they keep compiling
  until the classes are removed in 2.7.
- Run IntelliJ inspections on every touched file; fix warnings introduced by the change.
- Regenerate and eyeball `kotlin-gradle-plugin.api`.
- Follow the KGP area docs: `libraries/tools/kotlin-gradle-plugin/AGENTS.md` and integration-tests `AGENTS.md`.
