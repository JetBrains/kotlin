/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.nj2k

import com.intellij.openapi.application.runWriteAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.projectRoots.SdkModificator
import com.intellij.openapi.projectRoots.impl.JavaSdkImpl
import com.intellij.openapi.roots.LanguageLevelModuleExtension
import com.intellij.openapi.roots.ModifiableRootModel
import com.intellij.openapi.util.io.FileUtil
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.LightProjectDescriptor
import org.jetbrains.kotlin.idea.test.KotlinWithJdkAndRuntimeLightProjectDescriptor
import org.jetbrains.kotlin.test.InTextDirectivesUtils
import java.io.File

fun descriptorByFileDirective(testDataFile: File, isAllFilesPresentInTest: Boolean) =
    object : KotlinWithJdkAndRuntimeLightProjectDescriptor() {
        private fun projectDescriptorByFileDirective(): LightProjectDescriptor {
            if (isAllFilesPresentInTest) return KotlinWithJdkAndRuntimeLightProjectDescriptor.INSTANCE
            val fileText = FileUtil.loadFile(testDataFile, true)
            return if (InTextDirectivesUtils.isDirectiveDefined(fileText, "RUNTIME_WITH_FULL_JDK"))
                INSTANCE_FULL_JDK
            else INSTANCE
        }

        override fun getSdk(): Sdk? {
            val sdk = projectDescriptorByFileDirective().sdk ?: return null
            runWriteAction {
                val modificator: SdkModificator = sdk.sdkModificator
                JavaSdkImpl.attachJdkAnnotations(modificator)
                modificator.commitChanges()
            }
            return sdk
        }

        override fun configureModule(module: Module, model: ModifiableRootModel) {
            super.configureModule(module, model)
            model.getModuleExtension(LanguageLevelModuleExtension::class.java).languageLevel = LanguageLevel.JDK_1_8
        }
    }

