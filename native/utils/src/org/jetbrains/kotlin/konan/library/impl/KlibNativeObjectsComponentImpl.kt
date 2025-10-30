/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.library.components.KlibNativeObjectsComponent
import org.jetbrains.kotlin.konan.library.components.KlibNativeObjectsComponentLayout
import org.jetbrains.kotlin.library.KlibLayoutReader
import org.jetbrains.kotlin.konan.file.File as KlibFile

internal class KlibNativeObjectsComponentImpl(
    private val layoutReader: KlibLayoutReader<KlibNativeObjectsComponentLayout>
) : KlibNativeObjectsComponent {
    override val nativeObjectFilePaths: List<String> by lazy {
        layoutReader.readExtractingToTemp(KlibNativeObjectsComponentLayout::nativeObjectsDir).listFiles.map(KlibFile::absolutePath)
    }
}
