/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.cli.klib

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.library.impl.createKonanLibraryComponents
import org.jetbrains.kotlin.library.KotlinLibrary
import org.jetbrains.kotlin.library.KotlinLibrarySearchPathResolver

internal fun klibResolver(
    distributionKlib: String?,
    skipCurrentDir: Boolean,
    logger: KlibToolLogger,
): KotlinLibrarySearchPathResolver<KotlinLibrary> = object : KotlinLibrarySearchPathResolver<KotlinLibrary>(
    directLibs = emptyList(),
    distributionKlib,
    skipCurrentDir,
    logger
) {
    override fun libraryComponentBuilder(file: File, isDefault: Boolean) = createKonanLibraryComponents(file, null, isDefault)
}
