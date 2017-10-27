/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.idea.nodejs.protractor

import com.intellij.execution.actions.CompatibleRunConfigurationProducer
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.js.getJsOutputFilePath
import org.jetbrains.kotlin.idea.nodejs.getNodeJsEnvironmentVars
import org.jetbrains.kotlin.idea.project.TargetPlatformDetector
import org.jetbrains.kotlin.idea.util.projectStructure.getModuleDir
import org.jetbrains.kotlin.idea.util.projectStructure.module
import org.jetbrains.kotlin.js.resolve.JsPlatform

class KotlinProtractorRunConfigurationProducer :
        CompatibleRunConfigurationProducer<KotlinProtractorRunConfiguration>(KotlinProtractorConfigurationType.getInstance()) {
    override fun isConfigurationFromCompatibleContext(
            configuration: KotlinProtractorRunConfiguration,
            context: ConfigurationContext
    ): Boolean {
        val contextPsi = context.psiLocation ?: return false
        val module = contextPsi.module ?: return false
        val testFilePath = getJsOutputFilePath(module, isTests = true, isMeta = false) ?: return false
        return configuration.runSettings.testFileSystemDependentPath == FileUtil.toSystemDependentName(testFilePath)
    }

    override fun setupConfigurationFromCompatibleContext(
            configuration: KotlinProtractorRunConfiguration,
            context: ConfigurationContext,
            sourceElement: Ref<PsiElement>
    ): Boolean {
        val element = context.psiLocation ?: return false
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return false
        if (element !is PsiDirectory || module.getModuleDir() != element.virtualFile.path) return false
        if (TargetPlatformDetector.getPlatform(module) !is JsPlatform) return false
        val testFilePath = getJsOutputFilePath(module, true, false) ?: return false

        sourceElement.set(element)
        configuration.runSettings = configuration.runSettings.copy(
                testFilePath = testFilePath,
                envData = module.getNodeJsEnvironmentVars()
        )
        configuration.name = configuration.suggestedName()
        return true
    }
}