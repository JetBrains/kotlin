package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs


internal data class ModuleAndPackageDocumentationFragment(
    val name: String,
    val classifier: ModuleAndPackageDocumentation.Classifier,
    val documentation: String,
    val source: ModuleAndPackageDocumentationSource
)
