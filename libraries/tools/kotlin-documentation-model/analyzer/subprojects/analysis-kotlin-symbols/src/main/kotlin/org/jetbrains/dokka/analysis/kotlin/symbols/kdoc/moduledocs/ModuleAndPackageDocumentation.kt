/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs

import org.jetbrains.dokka.model.doc.DocumentationNode

internal data class ModuleAndPackageDocumentation(
    val name: String,
    val classifier: Classifier,
    val documentation: DocumentationNode
) {
    enum class Classifier { Module, Package }
}
