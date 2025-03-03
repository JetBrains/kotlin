/*
 * Copyright 2023-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// Copyright (C) 2021-2023 Brian Norman
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package org.jetbrains.kotlin.powerassert.diagram

import org.jetbrains.kotlin.cli.common.messages.CompilerMessageLocation
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.PsiIrFileEntry
import org.jetbrains.kotlin.ir.SourceRangeInfo
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.powerassert.earliestStartOffset
import org.jetbrains.kotlin.powerassert.getExplicitReceiver

data class SourceFile(
    val irFile: IrFile,
) {
    private val source = irFile.readSourceText()
        .replace("\r\n", "\n") // Normalize line endings in the same way the compiler does.

    fun getSourceRangeInfo(element: IrElement): SourceRangeInfo {
        return irFile.fileEntry.getSourceRangeInfo(element.startOffset, element.endOffset)
    }

    fun getCompleteSourceRangeInfo(element: IrElement): SourceRangeInfo {
        var range = element.startOffset..element.endOffset
        when (element) {
            is IrCall -> {
                val explicitReceiver = element.getExplicitReceiver()
                if (explicitReceiver != null) {
                    // When a function is called *not* with infix notation, the startOffset will not include the receiver.
                    // Force the range to include the receiver, so it is visible.
                    range = element.earliestStartOffset..element.endOffset

                    // The offsets of the receiver will *not* include surrounding parentheses,
                    // so these need to be checked for manually.
                    val substring = getText(range.first - 1, explicitReceiver.endOffset + 1)
                    if (substring.startsWith('(') && substring.endsWith(')')) {
                        range = (range.first - 1)..range.last
                    }
                }
            }
        }
        return irFile.fileEntry.getSourceRangeInfo(range.first, range.last)
    }

    fun getText(info: SourceRangeInfo): String {
        return getText(info.startOffset, info.endOffset)
    }

    fun getText(start: Int, end: Int): String {
        return source.substring(maxOf(start, 0), minOf(end, source.length))
    }

    fun getCompilerMessageLocation(element: IrElement): CompilerMessageLocation {
        val info = getSourceRangeInfo(element)
        val lineContent = getText(info)
        return CompilerMessageLocation.create(irFile.path, info.startLineNumber, info.startColumnNumber, lineContent)!!
    }
}

private fun IrFile.readSourceText(): String {
    // Try and access the source text via FIR metadata.
    val sourceFile = (metadata as? FirMetadataSource.File)?.fir?.sourceFile
    if (sourceFile != null) {
        return sourceFile.getContentsAsStream().use { it.reader().readText() }
    }

    // If FIR metadata isn't available, try and get source text via PSI.
    when (val entry = fileEntry) {
        is PsiIrFileEntry -> return entry.psiFile.text
    }

    error("Unable to find source for IrFile: $path")
}
