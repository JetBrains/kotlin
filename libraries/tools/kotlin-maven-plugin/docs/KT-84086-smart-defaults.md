# KT-84086: Configure smart defaults only from the module's Kotlin extension

Status: implemented on this branch. The one-version regression passes on Maven 3.6.3, 3.9.12, and 4.0.0-rc-5.

Audience: Kotlin Maven plugin maintainers implementing and reviewing [KT-84086](https://youtrack.jetbrains.com/issue/KT-84086).

## Design

The participant checks ownership before calling `configureSmartDefaults`. It may configure a module only when its defining classloader is an extension realm imported by that module's project realm.

Reuse Maven's existing deduplication of lifecycle participant instances. For the normal Maven lifecycle, ownership filtering is enough to make configuration run once per eligible module. Keep the source-root, stdlib, and JVM-target guards inside configuration: they preserve user choices.

Do not add a shared "already configured" flag as the primary fix. Such a flag would allow whichever participant runs first to configure every module, potentially using the wrong Kotlin implementation.

The realm check distinguishes the two owners on Maven 3.6.3, 3.9.12, and 4.0.0-rc-5. It uses Maven internals, isolated in `KotlinLifecycleParticipant.isProjectOwnedByThisParticipant`; embedded launch environments still need compatibility validation.

## Why one Kotlin version can configure a module twice

A Maven class realm is a classloader for a particular set of extension artifacts. The participant belongs to that realm, rather than to one module or one version string.

Maven sets up extension realms while building project models. Compatible extension artifact sets can reuse the same realm and participant instance. Adding a dependency to a plugin can create a separate realm even when the plugin version is unchanged.

Maven then collects lifecycle participants from the project realms, deduplicates their instances, and calls each participant's `afterProjectsRead` with the entire `MavenSession`. Before this fix, Kotlin checked only plugin presence, `extensions=true`, and whether smart defaults were enabled for each project.

The rewritten regression uses:

| Module        | Kotlin plugin version | Plugin dependencies                                                  | Participant |
| ------------- | --------------------- | -------------------------------------------------------------------- | ----------- |
| first-module  | `${kotlin.version}`   | Default dependencies                                                 | A           |
| second-module | `${kotlin.version}`   | Default dependencies plus `kotlin-maven-allopen` at the same version | B           |

All-open is a Kotlin compiler plugin that makes classes and members with selected annotations open for inheritance. Here its Maven artifact supplies a realistic difference in plugin dependencies. The `validate` phase reproduces the issue before compilation; all-open does not need to transform any source code.

Before the fix, both A and B configured both modules. With the ownership filter:

```text
first-module  -> project realm -> Kotlin extension realm A -> participant A
second-module -> project realm -> Kotlin extension realm B -> participant B

Maven calls A(session): configure first-module; skip second-module
Maven calls B(session): skip first-module; configure second-module
```

Several modules can import realm A; A must configure all of those eligible modules. A project realm can also import unrelated extensions, so direct equality between the project realm and the participant's classloader is incorrect.

## Existing guards and their limits

| Guard                                                     | What it protects                                                            | Why it cannot mark configuration complete                                               |
| --------------------------------------------------------- | --------------------------------------------------------------------------- | --------------------------------------------------------------------------------------- |
| Maven participant set                                     | Calls each discovered participant instance once during the phase            | Distinct extension realms contain distinct instances                                    |
| Source-directory checks and Maven root-list deduplication | Preserve explicit source directories and avoid identical root entries       | A valid configuration may add no roots; another implementation can add an unwanted root |
| Direct stdlib dependency check                            | Avoid adding another direct `org.jetbrains.kotlin:kotlin-stdlib` dependency | The user may have declared stdlib before any smart-default configuration                |
| Existing JVM target or release                            | Preserve explicit settings and previously derived values                    | The value may be user input, or no value may be derivable                               |
| `kotlin.smart.defaults.enabled`                           | Controls whether smart defaults should run                                  | It is user input, not execution state                                                   |

Stdlib's version comes from the plugin declaration in the module being configured. It is not taken from the executing participant's own artifact version. This explains why duplicate participants do not necessarily add different stdlib versions: the first adds the module's requested dependency, and the second sees that dependency and returns.

Those local guards make much of the current implementation idempotent. They do not stop entry into `configureSmartDefaults`, and they cannot prevent a different implementation from applying its own rules to the module.

## Architecture

### 1. Keep lifecycle orchestration separate from configuration

`afterProjectsRead` remains responsible for visiting session projects. For each project:

1. Find the effective Kotlin Maven plugin declaration.
2. Apply the existing extension and smart-default enablement checks.
3. Determine whether this participant owns the module.
4. Call `configureSmartDefaults` only for an owned module.

The ownership check must precede the announcement and all three mutations: source roots, stdlib dependency, and JVM target. Keep the announcement inside `configureSmartDefaults` so the regression continues to observe entry into configuration.

### 2. Isolate the ownership rule

The private `isProjectOwnedByThisParticipant` method receives a `MavenProject` and obtains the defining classloader from `KotlinLifecycleParticipant.class`. It compares imported realms by object identity.

For the supported Maven build-plugin extension layout, the core predicate is:

```text
project.getClassRealm().getImportRealms()
        .contains(KotlinLifecycleParticipant.class.getClassLoader())
```

The method returns `false` for missing realms or unmatched imports. `ClassRealm` inherits identity equality, so `getImportRealms().contains(participantRealm)` replaces a manual identity loop.

Maven's project-building code explicitly imports each extension realm into its project realm. When an extension exports no packages, Maven still records an import to make its components visible. This is why inspecting imported realms works, while asking the project realm to load the participant class is not a reliable substitute.

The debugger produced this matrix on each of the three tested Maven versions:

| Executing participant | first-module | second-module |
| --------------------- | ------------ | ------------- |
| A                     | Owns         | Does not own  |
| B                     | Does not own | Owns          |

This establishes the ownership predicate for the reproduced layout. It does not yet establish compatibility with every Maven embedder or extension packaging arrangement.

### 3. Reuse Maven's invocation guard

Maven already deduplicates discovered participant objects before invoking `afterProjectsRead`. Combined with module ownership, that supplies the once-per-module behavior for this lifecycle phase.

The fix needs no new static registry, project property, or completion flag. There is no new shared mutable state, locking protocol, or cleanup lifecycle. Existing configuration errors still fail the build; this change adds no retries.

If a supported embedder is later shown to call the same participant repeatedly for the same session, add a separate completion guard after ownership selection. That would require explicit session lifetime semantics, cross-classloader storage, and marking success only after the whole operation completes. A stdlib dependency or JVM-target property must never serve as that marker. Repeated callbacks have not been demonstrated as the cause of KT-84086.

### 4. Revisit the missing-realm fallback

`MavenProject.getClassRealm()` is documented by Maven as an internal utility that plugins must not rely on. Its use here is a deliberate compatibility dependency, not a public-API guarantee. Keep this dependency localized and cover every supported Maven version.

For now, missing or empty realm information returns `false`, leaving the project unchanged without throwing or logging a warning. Follow up after this implementation round: examine when supported Maven hosts omit realm information and decide whether this fallback needs a diagnostic or another ownership lookup.

Before shipping, verify IDE import and other supported embedded launch paths. If a supported host uses another topology, provide an explicit ownership adapter for it before enabling this rule there. A topology with populated but incompatible imports could otherwise cause every participant to skip the project.

An alternative is to inject `MavenPluginManager` and obtain the module's extension realm through `setupExtensionsRealm`. Maven's implementation first checks its project-context cache, and Maven itself reuses this method when preparing extension-plugin goals. However, the returned `ExtensionRealmCache.CacheRecord` is also documented as internal; cache misses can initiate extension setup. This alternative needs its own compatibility and side-effect validation. It is not required for the smaller, observed imported-realm predicate.

### 5. Preserve the configuration implementation

Do not change the rules for source roots, stdlib, or JVM target as part of this fix. Their individual guards are still needed on the first and only configuration pass.

Do not move this work into a compiler goal: smart defaults must affect the project before execution planning and dependency resolution. A `validate`-only build should continue to receive the defaults.

## Historical versions and a representative failure

A separate experiment combined the current Kotlin plugin with published 2.3.20-Beta2. One module explicitly selected a custom source directory. Both that directory and the excluded default Kotlin directory contained a class with the same fully qualified name.

The current participant respected the override. The Beta2 participant visited the other module and added its default Kotlin directory, producing a compilation failure with a redeclaration error. The control using the current implementation in both modules compiled successfully.

That experiment demonstrates why ownership matters beyond log output. It does not demonstrate a compilation failure caused by two copies of the same current implementation.

A newly fixed participant cannot stop an already published Beta2 participant from visiting every project. Neither an ownership filter nor a new completion flag can force an old artifact to cooperate. Full isolation requires all active Kotlin participants to contain the fix.

Use the one-version/all-open fixture for the permanent regression so it exercises the implementation under development in both realms. For an integration test with different implementations, build two controlled fixture artifacts that both implement ownership, or add that test when two fixed releases exist. Do not expect a reactor containing unmodified Beta2 to become fully isolated by changing only the current plugin.

## Validation

The rewritten `SmartDefaultsIT.testSmartDefaultsLoggedOncePerModule` runs `validate`, expects one announcement per child module and two in total, and uses a single Kotlin version with an all-open dependency only in the second module. It then compiles both modules with two Maven worker threads, checking that source roots and stdlib are available and that both classes target Java 17 as inherited from the parent POM.

Before the fix, the regression failed on Maven 3.6.3, 3.9.12, and 4.0.0-rc-5: expected 1, actual 2. With the fix, all three versions pass both the announcement and compilation checks.

The ownership suite has six unit tests. It loads real copies of the participant into separate ClassWorlds realms to verify that the first callback leaves foreign projects untouched, that multiple modules can share an owner, and that reversing the callback order preserves ownership. It also covers explicit stdlib, missing or empty realms, and disabled defaults. The existing 45 JVM-target and 10 source-directory unit tests pass.

The complete `SmartDefaultsIT` suite passes all 51 cases. The three existing multi-module JVM-target tests pass all nine Maven-version cases, covering shared realms, inherited settings, and child overrides. Separate `-pl second-module validate` checks on all three Maven versions configure only the selected module. None of these runs has failures or skipped cases.

ClassWorlds is already provided transitively by `maven-plugin-api`. The ownership tests use test-scoped `maven-compat` to supply the settings and resolver classes required by `MavenSession`; `maven-core` excludes its transitive dependencies in this module.

IDE model-import behavior has not been verified. The command-line tests and IDE-launched JUnit regression exercise Maven builds; they do not establish how every embedded importer handles project realms. This remains a compatibility check before release, as described above.

Alternative fixes to reject:

| Approach                                               | Problem                                                             |
| ------------------------------------------------------ | ------------------------------------------------------------------- |
| Deduplicate announcements                              | Both implementations still configure the project                    |
| Skip when stdlib or a target already exists            | Confuses user configuration with completion                         |
| One shared completion flag without ownership           | Makes callback order select the implementation                      |
| Compare Kotlin versions                                | Both participants in the rewritten regression have the same version |
| Static set inside the plugin                           | Each extension classloader gets separate static state               |
| Compare project realm directly with participant loader | Project and extension realms are different objects                  |
| Load the participant class through the project realm   | Component visibility does not imply exported Java classes           |

The acceptance criterion is that only the module's own Kotlin participant enters smart-default configuration, with each eligible module configured once in Maven's lifecycle phase.
