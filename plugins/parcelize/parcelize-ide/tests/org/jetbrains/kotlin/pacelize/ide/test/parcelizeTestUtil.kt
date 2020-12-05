/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pacelize.ide.test

import com.intellij.openapi.module.Module
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.test.KotlinTestUtils

fun addParcelizeLibraries(module: Module) {
    val androidJar = KotlinTestUtils.findAndroidApiJar()
    ConfigLibraryUtil.addLibrary(module, "androidJar", androidJar.parentFile.absolutePath, arrayOf(androidJar.name))
    ConfigLibraryUtil.addLibrary(module, "parcelizeRuntime", "dist/kotlinc/lib", arrayOf("parcelize-runtime.jar"))
    ConfigLibraryUtil.addLibrary(module, "androidExtensionsRuntime", "dist/kotlinc/lib", arrayOf("android-extensions-runtime.jar"))
}

fun removeParcelizeLibraries(module: Module) {
    ConfigLibraryUtil.removeLibrary(module, "androidJar")
    ConfigLibraryUtil.removeLibrary(module, "parcelizeRuntime")
    ConfigLibraryUtil.removeLibrary(module, "androidExtensionsRuntime")
}