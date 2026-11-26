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
     * The bitcode of [variant] for [target].
     */
    data class Kind(
        val target: KonanTarget,
        val variant: BitcodeVariant = BitcodeVariant.DEFAULT,
    ) : KlibComponent.Kind<KlibBitcodeComponent, KlibBitcodeComponentLayout> {
        override fun createLayout(root: Path) = KlibBitcodeComponentLayout(target, root, variant)

        override fun createComponentIfDataInKlibIsAvailable(layoutReader: KlibLayoutReader<KlibBitcodeComponentLayout>): KlibBitcodeComponent? =
            if (layoutReader.readInPlaceOrFallback(false) { it.bitcodeDir.exists() }) KlibBitcodeComponentImpl(layoutReader) else null
    }
}

/**
 * A variant of the bitcode for a target.
 */
enum class BitcodeVariant(internal val folderName: String?) {
    /** The bitcode that binaries of the target link. */
    DEFAULT(null),

    /** The bitcode compiled for arm64e, which binaries compiled with `-Xbinary=arm64e=true` link instead. */
    ARM64E(KLIB_ARM64E_FOLDER_NAME),
}

fun Klib.bitcode(target: KonanTarget, variant: BitcodeVariant = BitcodeVariant.DEFAULT): KlibBitcodeComponent? =
    getComponent(KlibBitcodeComponent.Kind(target, variant))

class KlibBitcodeComponentLayout(
    val target: KonanTarget,
    root: Path,
    val variant: BitcodeVariant = BitcodeVariant.DEFAULT,
) : KlibComponentLayout(root) {
    val bitcodeDir: Path
        get() = root.resolve(KLIB_DEFAULT_COMPONENT_NAME)
            .resolve(KLIB_TARGETS_FOLDER_NAME)
            .resolve(target.visibleName)
            .let { targetDir -> variant.folderName?.let(targetDir::resolve) ?: targetDir }
            .resolve(KLIB_BITCODE_FOLDER_NAME)
}

/** Constants for bitcode files stored in Native Klibs. */
object KlibBitcodeConstants {
    const val KLIB_BITCODE_FOLDER_NAME = "native"
    const val KLIB_ARM64E_FOLDER_NAME = "arm64e"

    /**
     * Lists the bitcode variants that the library has besides the default one. The only such variant so far is `arm64e`.
     */
    const val KLIB_PROPERTY_NATIVE_BITCODE_VARIANTS = "native_bitcode_variants"
}
