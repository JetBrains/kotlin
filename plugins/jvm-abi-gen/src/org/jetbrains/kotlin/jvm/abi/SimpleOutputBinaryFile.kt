/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.abi

import org.jetbrains.kotlin.backend.common.output.OutputFile
import java.io.File

class SimpleOutputBinaryFile(
    override val sourceFiles: List<File>,
    override val relativePath: String,
    private val content: ByteArray,
) : OutputFile {
    override val generatedForCompilerPlugin: Boolean
        get() = false

    override fun asByteArray(): ByteArray = content
    override fun asText(): String = String(content)

    override fun toString() = "$relativePath (compiled from $sourceFiles)"
}
