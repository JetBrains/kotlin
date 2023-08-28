package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs

import org.jetbrains.dokka.model.doc.DocumentationNode

internal data class ModuleAndPackageDocumentation(
    val name: String,
    val classifier: Classifier,
    val documentation: DocumentationNode
) {
    enum class Classifier { Module, Package }
}
