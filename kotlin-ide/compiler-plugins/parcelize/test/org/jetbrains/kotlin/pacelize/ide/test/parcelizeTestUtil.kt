/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.pacelize.ide.test

import com.intellij.openapi.application.PathManager
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.OrderRootType
import org.jetbrains.kotlin.idea.artifacts.AdditionalKotlinArtifacts
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.addRoot
import org.jetbrains.kotlin.test.KotlinTestUtils
import java.io.File

fun addParcelizeLibraries(module: Module) {
    ConfigLibraryUtil.addLibrary(module, "androidJar") {
        addRoot(File(PathManager.getHomePath(), "community/android/android/testData/android.jar"), OrderRootType.CLASSES)
    }
    ConfigLibraryUtil.addLibrary(module, "parcelizeRuntime") {
        addRoot(AdditionalKotlinArtifacts.parcelizeRuntime, OrderRootType.CLASSES)
    }
    ConfigLibraryUtil.addLibrary(module, "androidExtensionsRuntime") {
        addRoot(AdditionalKotlinArtifacts.androidExtensionsRuntime, OrderRootType.CLASSES)
    }

}

fun removeParcelizeLibraries(module: Module) {
    ConfigLibraryUtil.removeLibrary(module, "androidJar")
    ConfigLibraryUtil.removeLibrary(module, "parcelizeRuntime")
    ConfigLibraryUtil.removeLibrary(module, "androidExtensionsRuntime")
}