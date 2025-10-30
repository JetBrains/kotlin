/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.components

import org.jetbrains.kotlin.konan.library.components.KlibNativeConstants.KLIB_TARGETS_FOLDER_NAME
import org.jetbrains.kotlin.konan.library.components.KlibNativeObjectConstants.KLIB_NATIVE_OBJECTS_FOLDER_NAME
import org.jetbrains.kotlin.konan.library.impl.KlibNativeObjectsComponentImpl
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.library.Klib
import org.jetbrains.kotlin.library.KlibComponent
import org.jetbrains.kotlin.library.KlibComponentLayout
import org.jetbrains.kotlin.library.KlibLayoutReader
import org.jetbrains.kotlin.library.impl.KLIB_DEFAULT_COMPONENT_NAME
import org.jetbrains.kotlin.konan.file.File as KlibFile

interface KlibNativeObjectsComponent : KlibComponent {
    val nativeObjectFilePaths: List<String>

    data class Kind(val target: KonanTarget) : KlibComponent.Kind<KlibNativeObjectsComponent, KlibNativeObjectsComponentLayout> {
        override fun createLayout(root: KlibFile) = KlibNativeObjectsComponentLayout(target, root)

        override fun createComponentIfDataInKlibIsAvailable(layoutReader: KlibLayoutReader<KlibNativeObjectsComponentLayout>): KlibNativeObjectsComponent? {
            return if (layoutReader.readInPlaceOrFallback(false) { it.nativeObjectsDir.exists }) KlibNativeObjectsComponentImpl(layoutReader) else null
        }
    }
}

fun Klib.nativeObjects(target: KonanTarget): KlibNativeObjectsComponent? =
    getComponent(KlibNativeObjectsComponent.Kind(target))

class KlibNativeObjectsComponentLayout(val target: KonanTarget, root: KlibFile) : KlibComponentLayout(root) {
    val nativeObjectsDir: KlibFile
        get() = root.child(KLIB_DEFAULT_COMPONENT_NAME)
            .child(KLIB_TARGETS_FOLDER_NAME)
            .child(target.visibleName)
            .child(KLIB_NATIVE_OBJECTS_FOLDER_NAME)
}

object KlibNativeObjectConstants {
    const val KLIB_NATIVE_OBJECTS_FOLDER_NAME = "included"
}
