/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.components

import org.jetbrains.kotlin.konan.library.components.KlibBitcodeConstants.KLIB_ARM64E_FOLDER_NAME
import org.jetbrains.kotlin.konan.library.components.KlibBitcodeConstants.KLIB_BITCODE_FOLDER_NAME
import org.jetbrains.kotlin.library.components.KlibNativeConstants.KLIB_TARGETS_FOLDER_NAME
import org.jetbrains.kotlin.konan.library.impl.KlibBitcodeComponentImpl
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.Klib
import org.jetbrains.kotlin.library.KlibComponent
import org.jetbrains.kotlin.library.KlibComponentLayout
import org.jetbrains.kotlin.library.KlibConstants.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.library.KlibLayoutReader
import java.nio.file.Path
import kotlin.io.path.exists

interface KlibBitcodeComponent : KlibComponent {
    val bitcodeFilePaths: List<Path>

    /**
     * With [arm64e], the bitcode compiled for arm64e, which arm64e binaries of [target] link instead.
     */
    data class Kind(
        val target: KonanTarget,
        val arm64e: Boolean = false,
    ) : KlibComponent.Kind<KlibBitcodeComponent, KlibBitcodeComponentLayout> {
        override fun createLayout(root: Path) = KlibBitcodeComponentLayout(target, root, arm64e)

        override fun createComponentIfDataInKlibIsAvailable(layoutReader: KlibLayoutReader<KlibBitcodeComponentLayout>): KlibBitcodeComponent? =
            if (layoutReader.readInPlaceOrFallback(false) { it.bitcodeDir.exists() }) KlibBitcodeComponentImpl(layoutReader) else null
    }
}

fun Klib.bitcode(target: KonanTarget, arm64e: Boolean = false): KlibBitcodeComponent? =
    getComponent(KlibBitcodeComponent.Kind(target, arm64e))

class KlibBitcodeComponentLayout(val target: KonanTarget, root: Path, val arm64e: Boolean = false) : KlibComponentLayout(root) {
    val bitcodeDir: Path
        get() = root.resolve(KLIB_DEFAULT_COMPONENT_NAME)
            .resolve(KLIB_TARGETS_FOLDER_NAME)
            .resolve(target.visibleName)
            .let { if (arm64e) it.resolve(KLIB_ARM64E_FOLDER_NAME) else it }
            .resolve(KLIB_BITCODE_FOLDER_NAME)
}

/** Constants for bitcode files stored in Native Klibs. */
object KlibBitcodeConstants {
    const val KLIB_BITCODE_FOLDER_NAME = "native"
    const val KLIB_ARM64E_FOLDER_NAME = "arm64e"

    /**
     * Lists the variants of the bitcode the library contains besides the ordinary one: `arm64e`.
     */
    const val KLIB_PROPERTY_NATIVE_BITCODE_VARIANTS = "native_bitcode_variants"
}
