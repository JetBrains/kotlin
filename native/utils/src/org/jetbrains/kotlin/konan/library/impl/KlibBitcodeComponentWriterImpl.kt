/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.library.components.KlibBitcodeComponentLayout
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.writer.KlibComponentWriter
import org.jetbrains.kotlin.konan.file.File as KlibFile

internal class KlibBitcodeComponentWriterImpl(
    private val target: KonanTarget,
    private val bitcodeFilePaths: Collection<String>,
) : KlibComponentWriter {
    override fun writeTo(root: KlibFile) {
        val layout = KlibBitcodeComponentLayout(target, root)
        layout.bitcodeDir.mkdirs()

        for (filePath in bitcodeFilePaths) {
            val file = KlibFile(filePath)
            file.copyTo(layout.bitcodeDir.child(file.name))
        }
    }
}
