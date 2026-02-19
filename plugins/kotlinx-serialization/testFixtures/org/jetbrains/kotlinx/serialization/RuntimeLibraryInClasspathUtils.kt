/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.serialization

import org.jetbrains.kotlin.utils.PathUtil
import java.io.File

object RuntimeLibraryInClasspathUtils {
    val coreLibraryPath = getSerializationLibraryJar("kotlinx.serialization.KSerializer")
    val jsonLibraryPath = getSerializationLibraryJar("kotlinx.serialization.json.Json")

    private fun getSerializationLibraryJar(classToDetect: String): File? = try {
        PathUtil.getResourcePathForClass(Class.forName(classToDetect))
    } catch (e: ClassNotFoundException) {
        null
    }
}
