/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.components

import org.jetbrains.kotlin.konan.library.components.KlibNativeConstants.KLIB_TARGETS_FOLDER_NAME
import org.jetbrains.kotlin.konan.library.components.KlibNativeIncludedBinariesConstants.KLIB_NATIVE_INCLUDED_BINARIES_FOLDER_NAME
import org.jetbrains.kotlin.konan.library.impl.KlibNativeIncludedBinariesComponentImpl
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.Klib
import org.jetbrains.kotlin.library.KlibComponent
import org.jetbrains.kotlin.library.KlibComponentLayout
import org.jetbrains.kotlin.library.KlibLayoutReader
import org.jetbrains.kotlin.library.impl.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.konan.file.File as KlibFile

interface KlibNativeIncludedBinariesComponent : KlibComponent {
    val nativeIncludedBinaryFilePaths: List<String>

    data class Kind(val target: KonanTarget) : KlibComponent.Kind<KlibNativeIncludedBinariesComponent, KlibNativeIncludedBinariesComponentLayout> {
        override fun createLayout(root: KlibFile) = KlibNativeIncludedBinariesComponentLayout(target, root)

        override fun createComponentIfDataInKlibIsAvailable(layoutReader: KlibLayoutReader<KlibNativeIncludedBinariesComponentLayout>): KlibNativeIncludedBinariesComponent? {
            return if (layoutReader.readInPlaceOrFallback(false) { it.nativeIncludedBinariesDir.exists }) KlibNativeIncludedBinariesComponentImpl(layoutReader) else null
        }
    }
}

fun Klib.nativeIncludedBinaries(target: KonanTarget): KlibNativeIncludedBinariesComponent? =
    getComponent(KlibNativeIncludedBinariesComponent.Kind(target))

class KlibNativeIncludedBinariesComponentLayout(val target: KonanTarget, root: KlibFile) : KlibComponentLayout(root) {
    val nativeIncludedBinariesDir: KlibFile
        get() = root.child(KLIB_DEFAULT_COMPONENT_NAME)
            .child(KLIB_TARGETS_FOLDER_NAME)
            .child(target.visibleName)
            .child(KLIB_NATIVE_INCLUDED_BINARIES_FOLDER_NAME)
}

/** Constants for included Native binary files stored in Klibs. */
object KlibNativeIncludedBinariesConstants {
    const val KLIB_NATIVE_INCLUDED_BINARIES_FOLDER_NAME = "included"
}
