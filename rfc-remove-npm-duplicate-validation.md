☑️ Remove custom validation of duplicate npm dependencies

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

- **package.json dependency version**, the value npm reads for a dependency, e.g. `"react": "16.0.0"`. npm accepts a node-semver range
  here, and space-separated ranges are ANDed (`">=1.0.0 <2.0.0 >=1.5.0"` = the intersection).
- **node-semver**, the version/range grammar npm accepts. See https://github.com/npm/node-semver#versions
- **direct duplicate**, the same npm dependency declared more than once for one `package.json` (across source sets / scopes / `npm(...)`
  calls).
- **transitive clash**, the same npm package pulled in at different versions through imported Gradle node modules or tooling requirements.

☑️ What is this proposal solving?

When the same npm dependency is requested at more than one version, KGP today tries to reconcile the versions *itself*, in two places:

1. **Direct duplicates**, `PackageJson.chooseVersion(...)` (`PackageJson.kt`). It intersects the two node-semver ranges via the (now
   deprecated) `NpmRange` algebra (`includedRange(old) intersect includedRange(new)`) and, when they do not intersect, fails the build:

   ```
   There is already declared version of '<module>' with version '<old>' which does not intersects with another declared version '<new>'
   ```

2. **Transitive clashes**, `KotlinCompilationNpmResolution.disambiguateDependencies(...)`
   (`.../js/npm/resolver/KotlinCompilationNpmResolution.kt`). It groups transitive deps by name and, per name, picks the **maximum**
   version via `SemVer.from(dep.version, true)`, logging a warning listing the discarded candidates.

## Problems

- Both mechanisms **duplicate work npm already does**. npm resolves and validates dependency versions natively; KGP reimplements a slice of
  that on top of the custom `NpmRange` / `SemVer` machinery (see the companion RFC that removes that DSL).
- `chooseVersion` **fails the build at configuration/preparation time** for inputs npm would accept or would reject with a far clearer,
  install-time error. Non-intersecting ranges are legal to *write* to `package.json`; only the package manager can authoritatively decide
  whether an install solution exists.
- `disambiguateDependencies` **silently changes the user's request** (picks the max, drops the rest) and only warns. The chosen version may
  not be what any consumer actually needs, and warnings are easy to miss.
- Both keep the deprecated `NpmRange` / `SemVer` code alive as a runtime dependency, blocking its removal.

## Outcome

KGP stops reconciling duplicate npm versions. It **concatenates the requested versions space-separated** into the single `package.json`
dependency value and lets npm resolve/validate them, surfacing npm's own error if no solution exists.

## Assumptions

- The companion effort ("Replace the NPM override DSL with plain version strings") deprecates the `NpmRange` / `SemVer` machinery. Removing
  these two validators is what makes the `NpmRange`-based `chooseVersion` and the `SemVer`-based `disambiguateDependencies` dead, so the two
  proposals are complementary and land together.

### Anti-Goals

- *Not changing the `package.json` schema or field semantics.* Only the value KGP computes for a duplicated dependency changes (from
  "reconciled single range" to "space-joined ranges").
- *Not adding new KGP-side validation.* The whole point is to delegate validation to npm.
- *Not touching transitive-module dedup for imported Gradle node modules* (`NpmImportedPackagesVersionResolver`, which dedups workspace
  paths) - that is a separate concern from the per-dependency version reconciliation removed here.

## Solution (overview)

- **Direct duplicates**: in `packageJson(...)` (`PackageJson.kt`) accumulate every requested version for a name and join with a space, then
  write that as the dependency value:

  ```kotlin
  npmDependencies.forEach { (_, name, version) ->
      val oldVersion = dependencies[name]
      dependencies[name] = listOfNotNull(oldVersion, version).joinToString(" ")
  }
  ```

  Delete `chooseVersion(...)` entirely (it is already unused / commented out in the prototype).

- **Transitive clashes**: in `KotlinCompilationNpmResolution.prepare()` pass the dependencies straight through instead of disambiguating:

  ```kotlin
  externalNpmDependencies = externalNpmDependencies + otherNpmDependencies
  ```

  Delete `disambiguateDependencies(...)` and its call site. Duplicates then flow into `packageJson(...)` and are space-joined by the rule
  above, so the two paths converge on one behavior.

Net effect: a dependency requested as `^1.0.0` and `>=1.2.0` is written as `"^1.0.0 >=1.2.0"`; npm intersects it. If the user requested
genuinely incompatible versions, npm reports it at install with its standard, actionable error instead of KGP throwing during the build or
quietly picking one.

## Solution

### Key files

- `.../js/npm/PackageJson.kt` - `packageJson(...)` space-join (keep); remove `chooseVersion(...)`.
- `.../js/npm/resolver/KotlinCompilationNpmResolution.kt` - `prepare()` pass-through (keep); remove `disambiguateDependencies(...)` and its
  call.

### Interaction with `SemVer` / `NpmRange` removal

`chooseVersion` was the last caller of `includedRange`/`intersect` on the npm path; `disambiguateDependencies` was a caller of
`SemVer.from`.
Removing both clears the way for deprecating/removing that machinery (tracked by the companion RFC). No new code depends on it.

### Ordering guarantee

The space-join must be **deterministic** so `package.json` (and therefore up-to-date checks / build caching) is stable across runs. The
value is built by iterating `npmDependencies` in its existing declaration order; confirm that order is stable (it is derived from an ordered
collection, not a `HashSet`). If not, sort the joined tokens before writing.

☑️ Related work

- "Replace the NPM override DSL with plain version strings" (`rfc-npm-yarn-version-strings.md`) - removes the `override(...)` DSL and
  deprecates the same `NpmRange`/`SemVer` machinery. This RFC removes the *other* consumers of that machinery.
- KT-XXXXX - tracking issue (to be filed).

☑️ Prior Art

- **Keep `chooseVersion` (intersect + fail)** - rejected: fails at the wrong layer for inputs npm can adjudicate, and reimplements
  node-semver range intersection.
- **Keep `disambiguateDependencies` (pick max + warn)** - rejected: silently rewrites the user's request and hides the conflict behind a
  warning.
- **Fail fast on any duplicate** - rejected: duplicates across source sets are common and legitimate; npm's intersection semantics handle
  them.
- **Do nothing** - keeps two bespoke reconcilers and blocks `SemVer`/`NpmRange` removal.

☑️ FAQ

Q: Won't space-joining produce invalid `package.json` for weird inputs?
A: Space-separated ranges are valid node-semver (ANDed). KGP JSON-escapes the value, so the file stays valid JSON; whether a *solution*
exists for the combined range is npm's call, reported at install time.

Q: What replaces the build-time error users saw before?
A: npm's install-time error (e.g. `No matching version found` / `ETARGET`), which names the package and the unsatisfiable constraint -
more actionable than KGP's generic "does not intersect" message.

Q: Is this a behavior change users will notice?
A: Yes, in two edge cases: (1) previously-failing non-intersecting *direct* declarations now surface at install instead of build; (2)
transitive clashes that were silently resolved to the max version are now all forwarded to npm. Both move authority to the tool that owns
resolution. Call this out in release notes.

☑️ Errata

<none yet - draft>

---

## Verification (implementation checklist)

- Extend `Kotlin2JsGradlePluginIT` (or add a focused test) with a project that declares the same npm dependency at two compatible ranges;
  assert `package.json` contains the space-joined value and that `kotlinNpmInstall` succeeds.
- Add a negative test: two genuinely incompatible versions → assert the build *no longer* fails at preparation and that the failure (if any)
  originates from npm at install time.
- Grep the codebase to confirm `chooseVersion` and `disambiguateDependencies` have no remaining references after removal.
- Run IntelliJ inspections on the two touched files; fix warnings introduced by the change.
- Confirm `package.json` output is byte-stable across two consecutive builds (ordering guarantee).
- Follow the KGP area docs: `libraries/tools/kotlin-gradle-plugin/AGENTS.md` and integration-tests `AGENTS.md`.
- 
