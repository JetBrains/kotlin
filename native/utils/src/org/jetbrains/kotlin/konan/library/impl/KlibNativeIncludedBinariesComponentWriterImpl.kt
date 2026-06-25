/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.library.components.KlibNativeIncludedBinariesComponentLayout
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.writer.KlibComponentWriter
import java.nio.file.Path
import kotlin.io.path.Path
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.name

internal class KlibNativeIncludedBinariesComponentWriterImpl(
    private val target: KonanTarget,
    private val nativeIncludedBinaryFilePaths: Collection<String>,
) : KlibComponentWriter {
    override fun writeTo(root: Path) {
        val layout = KlibNativeIncludedBinariesComponentLayout(target, root)
        layout.nativeIncludedBinariesDir.createDirectories()

        for (filePath in nativeIncludedBinaryFilePaths) {
            val file = Path(filePath)
            file.copyTo(layout.nativeIncludedBinariesDir.resolve(file.name), overwrite = true)
        }
    }
}
