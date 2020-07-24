/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.test

import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.roots.OrderRootType
import org.jetbrains.kotlin.idea.artifacts.AdditionalKotlinArtifacts
import org.jetbrains.kotlin.idea.artifacts.KotlinArtifacts

class KotlinJdkAndMultiplatformStdlibDescriptor private constructor(private val withSources: Boolean) : KotlinLightProjectDescriptor() {
    override fun getSdk(): Sdk? = PluginTestCaseBase.mockJdk8()

    override fun configureModule(module: Module, model: ModifiableRootModel) {
        ConfigLibraryUtil.addLibrary(model, STDLIB_COMMON_LIB_NAME) {
            addRoot(AdditionalKotlinArtifacts.kotlinStdlibCommon, OrderRootType.CLASSES)
            addRoot(AdditionalKotlinArtifacts.kotlinStdlibCommonSources, OrderRootType.SOURCES)
        }

        ConfigLibraryUtil.addLibrary(model, STDLIB_LIB_NAME) {
            addRoot(KotlinArtifacts.instance.kotlinStdlib, OrderRootType.CLASSES)
            addRoot(KotlinArtifacts.instance.kotlinStdlibSources, OrderRootType.SOURCES)
        }
    }

    companion object {
        val JDK_AND_MULTIPLATFORM_STDLIB_WITH_SOURCES = KotlinJdkAndMultiplatformStdlibDescriptor(true)

        private const val STDLIB_COMMON_LIB_NAME = "stdlib-common"
        private const val STDLIB_LIB_NAME = "stdlib"
    }
}