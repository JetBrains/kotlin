
Reachability metadata collected for comparison.
Collection benchmark: mt-kotlin2/test-project-model-dump + modularized fir test

* [reachability-metadata-old.json](reachability-metadata-old.json) 
  * Collected by Kotlin 2.3-snapshot (Oct 2025 master)
  * `TOTAL MODULES: 319 OK MODULES: 306 FAILED MODULES: 13`
* [reachability-metadata-new.json](reachability-metadata-new.json)
  * Collected by the current branch
  * `TOTAL MODULES: 319 OK MODULES: 300 FAILED MODULES: 19`
  * New kind of errors for pipeline test, notably: `java.lang.NoSuchMethodError: 'void org.jetbrains.kotlin.compiler.plugin.CompilerPluginRegistrar$ExtensionStorage.registerExtension`
