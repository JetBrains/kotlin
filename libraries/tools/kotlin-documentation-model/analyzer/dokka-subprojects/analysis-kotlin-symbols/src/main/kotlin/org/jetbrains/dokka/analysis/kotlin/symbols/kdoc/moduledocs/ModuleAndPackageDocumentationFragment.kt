/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs


internal data class ModuleAndPackageDocumentationFragment(
    val name: String,
    val classifier: ModuleAndPackageDocumentation.Classifier,
    val documentation: String,
    val source: ModuleAndPackageDocumentationSource
)
