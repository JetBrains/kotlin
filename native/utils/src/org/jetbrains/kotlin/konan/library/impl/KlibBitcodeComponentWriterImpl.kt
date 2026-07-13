/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.library.components.KlibBitcodeComponentLayout
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.writer.KlibComponentWriter
import java.nio.file.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.name

internal class KlibBitcodeComponentWriterImpl(
    private val target: KonanTarget,
    private val bitcodeFilePaths: Collection<Path>,
) : KlibComponentWriter {
    override fun writeTo(root: Path) {
        val layout = KlibBitcodeComponentLayout(target, root)
        layout.bitcodeDir.createDirectories()

        for (filePath in bitcodeFilePaths) {
            filePath.copyTo(layout.bitcodeDir.resolve(filePath.name), overwrite = true)
        }
    }
}
