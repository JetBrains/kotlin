/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.library.components.KlibBitcodeComponent
import org.jetbrains.kotlin.konan.library.components.KlibBitcodeComponentLayout
import org.jetbrains.kotlin.library.KlibLayoutReader
import java.nio.file.Path
import kotlin.io.path.absolute
import kotlin.io.path.listDirectoryEntries

internal class KlibBitcodeComponentImpl(
    private val layoutReader: KlibLayoutReader<KlibBitcodeComponentLayout>
) : KlibBitcodeComponent {
    override val bitcodeFilePaths: List<Path> by lazy {
        layoutReader.readExtractingToTemp(KlibBitcodeComponentLayout::bitcodeDir)
            .listDirectoryEntries()
            .map(Path::absolute)
    }
}
