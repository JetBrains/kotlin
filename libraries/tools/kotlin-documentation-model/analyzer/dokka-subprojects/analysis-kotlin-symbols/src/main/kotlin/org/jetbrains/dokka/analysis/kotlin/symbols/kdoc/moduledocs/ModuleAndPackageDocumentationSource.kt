/*
 * Copyright 2014-2023 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.kdoc.moduledocs

import java.io.File

internal abstract class ModuleAndPackageDocumentationSource {
    abstract val sourceDescription: String
    abstract val documentation: String
    override fun toString(): String = sourceDescription
}

internal data class ModuleAndPackageDocumentationFile(private val file: File) : ModuleAndPackageDocumentationSource() {
    override val sourceDescription: String = file.path
    override val documentation: String by lazy(LazyThreadSafetyMode.PUBLICATION) { file.readText() }
}
