/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.library.components.KlibNativeIncludedBinariesComponent
import org.jetbrains.kotlin.konan.library.components.KlibNativeIncludedBinariesComponentLayout
import org.jetbrains.kotlin.library.KlibLayoutReader
import org.jetbrains.kotlin.konan.file.File as KlibFile

internal class KlibNativeIncludedBinariesComponentImpl(
    private val layoutReader: KlibLayoutReader<KlibNativeIncludedBinariesComponentLayout>
) : KlibNativeIncludedBinariesComponent {
    override val nativeIncludedBinaryFilePaths: List<String> by lazy {
        layoutReader.readExtractingToTemp(KlibNativeIncludedBinariesComponentLayout::nativeIncludedBinariesDir).listFiles.map(KlibFile::absolutePath)
    }
}
