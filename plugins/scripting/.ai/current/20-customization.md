# Current — Customization Layer

User-facing API + how it plugs into the compiler.

## API surface (`libraries/scripting/common/`)

| Type | Role |
|---|---|
| `ScriptCompilationConfiguration` | Container for compilation properties. Extends `PropertiesCollection`. Keys: classpath, jdkHome, defaultImports, baseClass, implicitReceivers, providedProperties, refineConfiguration, etc. |
| `ScriptEvaluationConfiguration` | Eval-time props (constructor args, implicit receivers, script wrapper). |
| `ScriptCompiler` | `suspend operator fun invoke(script, config): ResultWithDiagnostics<CompiledScript>` |
| `CompiledScript` | Output. `getClass(scriptEvaluationConfiguration)` returns the compiled class. |
| `ScriptEvaluator` | `suspend operator fun invoke(compiled, config): ResultWithDiagnostics<EvaluationResult>` |
| `ReplCompiler<CompiledSnippetT>` | Snippet compilation: `compile(snippets, config) → LinkedSnippet<CompiledSnippet>` |
| `ReplEvaluator<CompiledSnippetT, EvaluatedSnippetT>` | Snippet eval. |
| `SourceCode` | `text`, `name`, `locationId`. |
| `ResultWithDiagnostics<T>` | success/failure + diagnostics. |
| `RefineScriptCompilationConfigurationHandler` | typealias for refinement callback. |

## Refinement DSL

Inside `ScriptCompilationConfiguration { ... }`:

```
refineConfiguration {
    beforeParsing { handler }           // before parser runs
    onAnnotations(annotationTypes)      // when listed annotations are seen in @file/@-script
    onAnnotations<T : Annotation>       // typed variant
    beforeCompiling { handler }         // after parse, before compile
}
refineConfigurationBeforeEvaluate { handler }   // before eval
scriptExecutionWrapper { block -> ... }         // wraps user script execution
```

Handler signature: `(ScriptConfigurationRefinementContext) → ResultWithDiagnostics<ScriptCompilationConfiguration>`.

Helpers in `common/api/scriptCompilation.kt`: `refineBeforeParsing()`, `refineOnAnnotations()`, `refineBeforeCompiling()`, `refineBeforeEvaluation()`.

## Wiring into the compiler

### K2 (active)

Refinement config reaches FIR via:

1. **Host setup** populates `ScriptCompilationConfigurationProvider` + `ScriptRefinedCompilationConfigurationCache` in host configuration.
2. **Plugin registrar** registers `FirScriptingCompilerExtensionRegistrar`, which wires:
   - `FirScriptConfiguratorExtensionImpl` — uses config at FIR-build time to set baseClass, receivers, provided props.
   - `FirScriptResolutionConfigurationExtensionImpl` — feeds refined config during resolve.
   - `FirScriptDefinitionProviderService` — session-scoped holder.

### K1 (legacy)

- `ScriptDefinitionProvider` + `ScriptConfigurationsProvider` registered as project services.
- `ScriptingResolveExtension` + `ScriptExtraImportsProviderExtension` apply config during K1 resolve.

## Extension point summary

| EP | Where | Phase | Used for |
|---|---|---|---|
| `FirScriptConfiguratorExtension` | raw-fir.common | Raw FIR build | Set base class, receivers, params on `FirScript` |
| `FirReplSnippetConfiguratorExtension` | raw-fir.common | Raw FIR build | Build snippet class + eval fn body |
| `FirScriptResolutionConfigurationExtension` | fir/resolve | FIR resolve | Provide refined config |
| `FirReplSnippetResolveExtension` | fir/providers | FIR resolve | History scope + default imports |
| `Fir2IrScriptConfiguratorExtension` | fir/fir2ir | FIR→IR | IR script setup |
| `Fir2IrReplSnippetConfiguratorExtension` | fir/fir2ir | FIR→IR | IR snippet setup |

## Customization carriers used by main-kts

See [50-script-definitions.md](50-script-definitions.md). Highlights:
- `@DependsOn`, `@Repository` — Maven artifact resolution via `MavenDependenciesResolver`
- `@Import` — pulls in other scripts
- `@CompilerOptions` — adds CLI flags
- `@KotlinScript(fileExtension = "main.kts")` — script definition declaration

## What's NOT customizable today

- Snippet eval-fn shape is fixed (`$$eval`).
- Snippet-class wrapping is hard-coded in `FirReplSnippetConfiguratorExtensionImpl`.
- Result property name (`$$result`) is fixed.
- No public hook between FIR-resolve and FIR2IR.
