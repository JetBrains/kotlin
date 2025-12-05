/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.components

import org.jetbrains.kotlin.konan.library.components.KlibBitcodeConstants.KLIB_BITCODE_FOLDER_NAME
import org.jetbrains.kotlin.library.components.KlibNativeConstants.KLIB_TARGETS_FOLDER_NAME
import org.jetbrains.kotlin.konan.library.impl.KlibBitcodeComponentImpl
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.Klib
import org.jetbrains.kotlin.library.KlibComponent
import org.jetbrains.kotlin.library.KlibComponentLayout
import org.jetbrains.kotlin.library.KlibLayoutReader
import org.jetbrains.kotlin.library.impl.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.konan.file.File as KlibFile

interface KlibBitcodeComponent : KlibComponent {
    val bitcodeFilePaths: List<String>

    data class Kind(val target: KonanTarget) : KlibComponent.Kind<KlibBitcodeComponent, KlibBitcodeComponentLayout> {
        override fun createLayout(root: KlibFile) = KlibBitcodeComponentLayout(target, root)

        override fun createComponentIfDataInKlibIsAvailable(layoutReader: KlibLayoutReader<KlibBitcodeComponentLayout>): KlibBitcodeComponent? =
            if (layoutReader.readInPlaceOrFallback(false) { it.bitcodeDir.exists }) KlibBitcodeComponentImpl(layoutReader) else null
    }
}

fun Klib.bitcode(target: KonanTarget): KlibBitcodeComponent? =
    getComponent(KlibBitcodeComponent.Kind(target))

class KlibBitcodeComponentLayout(val target: KonanTarget, root: KlibFile) : KlibComponentLayout(root) {
    val bitcodeDir: KlibFile
        get() = root.child(KLIB_DEFAULT_COMPONENT_NAME)
            .child(KLIB_TARGETS_FOLDER_NAME)
            .child(target.visibleName)
            .child(KLIB_BITCODE_FOLDER_NAME)
}

/** Constants for bitcode files stored in Native Klibs. */
object KlibBitcodeConstants {
    const val KLIB_BITCODE_FOLDER_NAME = "native"
}
