# Current — Script Definitions

How `.kts` files are mapped to compilation/evaluation configurations.

## `ScriptDefinition`

A `ScriptDefinition` is a runtime object pairing:
- A `ScriptCompilationConfiguration` (base class, default imports, refinement, file extension, etc.)
- A `ScriptEvaluationConfiguration`
- Metadata: `fileExtension`, `hostConfiguration`

User declares one by annotating a Kotlin class with `@KotlinScript(fileExtension = "...")` and supplying the compilation/eval configs.

## Providers

| Provider | File | Frontend | Role |
|---|---|---|---|
| `FirScriptDefinitionProviderService` | `plugins/scripting/scripting-compiler/src/.../services/FirScriptDefinitionProviderService.kt` | K2 | Session-scoped FIR component. Holds compilation/eval providers + refined-config cache. Bridges deprecated `ScriptDefinitionProvider` to host-config model. PSI-free. |
| `CliScriptDefinitionProvider` | `plugins/scripting/scripting-compiler/src/.../definitions/CliScriptDefinitionProvider.kt` | both | Extends `LazyScriptDefinitionProvider`. Thread-safe (`ReentrantLock`). Two setters: `setScriptDefinitions()`, `setScriptDefinitionsSources()`. Transitional. |
| `ScriptDefinitionsFromClasspathDiscoverySource` | `plugins/scripting/scripting-compiler-impl/src/.../definitions/ScriptiDefinitionsFromClasspathDiscoverySource.kt` | both | Discovers definitions from classpath jars. **Deprecated (KT-82551)** but functional. |
| `ScriptConfigurationsProvider` | scripting-compiler-impl | K1 | Wraps `ScriptDefinitionProvider`. |

## Discovery via classpath markers

| Marker dir | Marker file | Content |
|---|---|---|
| `META-INF/kotlin/script/templates/` (inside jar) | `<FQN>.classname` | empty marker — FQN is the path-encoded class name |

Example: `META-INF/kotlin/script/templates/org.jetbrains.kotlin.mainKts.MainKtsScript.classname` triggers loading `MainKtsScript`.

Loader: `URLClassLoader` over the discovery classpath, lists `.classname` files, loads each class, instantiates via reflection.

## `kotlin-main-kts` — canonical example

`libraries/tools/kotlin-main-kts/src/.../mainKts/scriptDef.kt`:

| Element | Role |
|---|---|
| `MainKtsScript` | Abstract script base class. |
| `@KotlinScript(fileExtension = "main.kts")` | Definition annotation on `MainKtsScript`. |
| `MainKtsScriptDefinition` | `ScriptCompilationConfiguration`. Default imports include `DependsOn`, `Repository`, `Import`, `CompilerOptions`, `ScriptFileLocation`. |
| `MainKtsEvaluationConfiguration` | `ScriptEvaluationConfiguration`. |
| `MainKtsHostConfiguration` | Host config — cache dir handling. |
| `MainKtsConfigurator()` | `refineConfiguration { beforeCompiling { ... } }` — resolves Maven deps via `MavenDependenciesResolver`, applies `@Import` chain. |
| `ScriptFileLocationCustomConfigurator()` | Injects script-file location var. |

### Auto-loading

`META-INF/kotlin/script/templates/org.jetbrains.kotlin.mainKts.MainKtsScript.classname` ships in the main-kts jar. Picked up by both:
- CLI plugin autoload (when `kotlin-main-kts.jar` is on classpath)
- Gradle discovery transform

### Caching

`CompiledScriptJarsCache` keyed off source hash. Cache dir from env var `KOTLIN_MAIN_KTS_COMPILED_SCRIPTS_CACHE_DIR`.

## Where definitions come from in pipeline

| Path | Source |
|---|---|
| CLI compile | `loadCompilerPlugins()` reads classpath + `-script-templates` arg; populates `CliScriptDefinitionProvider`. |
| Gradle | Discovery transform + KGP plumbing → KGP forwards file extensions to compiler. |
| Embedded host | Caller provides `ScriptDefinition` directly via host configuration. |
| JSR-223 | `KotlinJsr223DefaultScriptEngineFactory` uses `KotlinJsr223DefaultScript`. |

## K1 quirk

K1 also has `ScriptingResolveExtension` (`SyntheticResolveExtension`) reading the same definitions to inject synthetic types. Goes with K1.
