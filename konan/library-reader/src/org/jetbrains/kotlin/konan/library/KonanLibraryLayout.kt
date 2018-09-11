/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.konan.library

import org.jetbrains.kotlin.konan.file.File
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * This scheme describes the Kotlin/Native Library (KLIB) layout.
 */
interface KonanLibraryLayout {

    val libraryName: String
    val libDir: File

    val source: KonanLibrarySource

    // This is a default implementation. Can't make it an assignment.
    val target: KonanTarget? get() = null

    val manifestFile get() = File(libDir, "manifest")
    val resourcesDir get() = File(libDir, "resources")

    val targetsDir get() = File(libDir, "targets")
    val targetDir get() = File(targetsDir, target!!.visibleName)

    val kotlinDir get() = File(targetDir, "kotlin")
    val nativeDir get() = File(targetDir, "native")
    val includedDir get() = File(targetDir, "included")

    val linkdataDir get() = File(libDir, "linkdata")
    val moduleHeaderFile get() = File(linkdataDir, KLIB_MODULE_METADATA_FILE_NAME)
    val dataFlowGraphFile get() = File(linkdataDir, "module_data_flow_graph")

    fun packageFragmentFile(packageFqName: String) =
        File(
            linkdataDir,
            if (packageFqName == "") "root_package.$KLIB_METADATA_FILE_EXTENSION" else "package_$packageFqName.$KLIB_METADATA_FILE_EXTENSION"
        )
}

sealed class KonanLibrarySource {
    object KonanLibraryDir : KonanLibrarySource()
    class KonanLibraryFile(val klibFile: File) : KonanLibrarySource()
}
