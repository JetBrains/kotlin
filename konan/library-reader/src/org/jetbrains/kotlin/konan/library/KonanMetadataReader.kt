/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.metadata.konan.KonanProtoBuf

interface MetadataReader {
    fun loadSerializedModule(libraryLayout: KonanLibraryLayout): KonanProtoBuf.LinkDataLibrary
    fun loadSerializedPackageFragment(libraryLayout: KonanLibraryLayout, packageFqName: String): KonanProtoBuf.LinkDataPackageFragment
}
