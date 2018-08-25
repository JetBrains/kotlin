/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library.impl

import org.jetbrains.kotlin.konan.library.KonanLibraryLayout
import org.jetbrains.kotlin.konan.library.MetadataReader

internal object DefaultMetadataReaderImpl : MetadataReader {

    override fun loadSerializedModule(libraryLayout: KonanLibraryLayout): ByteArray =
        libraryLayout.moduleHeaderFile.readBytes()

    override fun loadSerializedPackageFragment(libraryLayout: KonanLibraryLayout, fqName: String): ByteArray =
        libraryLayout.packageFile(fqName).readBytes()
}
