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
    val target: KonanTarget?
        // This is a default implementation. Can't make it an assignment.
        get() = null

    val manifestFile
        get() = File(libDir, "manifest")
    val resourcesDir
        get() = File(libDir, "resources")

    val targetsDir
        get() = File(libDir, "targets")
    val targetDir
        get() = File(targetsDir, target!!.visibleName)

    val kotlinDir
        get() = File(targetDir, "kotlin")
    val nativeDir
        get() = File(targetDir, "native")
    val includedDir
        get() = File(targetDir, "included")

    val linkdataDir
        get() = File(libDir, "linkdata")
    val moduleHeaderFile
        get() = File(linkdataDir, "module")
    val dataFlowGraphFile
        get() = File(linkdataDir, "module_data_flow_graph")

    fun packageFile(packageName: String) = File(linkdataDir, if (packageName == "") "root_package.knm" else "package_$packageName.knm")
}
