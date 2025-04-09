## 2.1.20

### Analysis API

#### Fixes

- [`KT-68198`](https://youtrack.jetbrains.com/issue/KT-68198) Analysis API: Support application service registration in plugin XMLs
- [`KT-57733`](https://youtrack.jetbrains.com/issue/KT-57733) Analysis API: Use optimized `ModuleWithDependenciesScope`s in combined symbol providers
- [`KT-73156`](https://youtrack.jetbrains.com/issue/KT-73156) AA: type retrieval for erroneous typealias crashes
- [`KT-71907`](https://youtrack.jetbrains.com/issue/KT-71907) K2 debugger evaluator failed when cannot resolve unrelated annotation
- [`KT-69128`](https://youtrack.jetbrains.com/issue/KT-69128) K2 IDE: "Unresolved reference in KDoc" reports existing Java class in reference to its own nested class
- [`KT-71613`](https://youtrack.jetbrains.com/issue/KT-71613) KaFirPsiJavaTypeParameterSymbol cannot be cast to KaFirTypeParameterSymbol
- [`KT-71741`](https://youtrack.jetbrains.com/issue/KT-71741) K2 IDE. Classifier was found in KtFile but was not found in FirFile in `libraries/tools/kotlin-gradle-plugin-integration-tests/build.gradle.kts` in `kotlin.git` and broken analysis
- [`KT-71942`](https://youtrack.jetbrains.com/issue/KT-71942) Need to rethrow Intellij Platform exceptions, like ProcessCanceledException
- [`KT-70949`](https://youtrack.jetbrains.com/issue/KT-70949) Analysis API: "containingDeclaration" does not work on nested Java classes in K2 implementation
- [`KT-69736`](https://youtrack.jetbrains.com/issue/KT-69736) K2 IDE: False positive resolution from KDoc for `value`
- [`KT-69047`](https://youtrack.jetbrains.com/issue/KT-69047) Analysis API: Unresolved KDoc reference to extensions with the same name
- [`KT-70815`](https://youtrack.jetbrains.com/issue/KT-70815) Analysis API: Implement stop-the-world session invalidation
- [`KT-69630`](https://youtrack.jetbrains.com/issue/KT-69630) KAPT User project builds with KAPT4 enabled fail with Metaspace overflow

### Analysis API. Code Compilation

- [`KT-71263`](https://youtrack.jetbrains.com/issue/KT-71263) K2 evaluator: Error in evaluating self property with extension receiver
