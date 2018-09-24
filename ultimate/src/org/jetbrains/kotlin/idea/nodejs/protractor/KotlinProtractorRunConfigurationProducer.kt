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
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.js.KotlinJSRunConfigurationData
import org.jetbrains.kotlin.idea.js.KotlinJSRunConfigurationDataProvider
import org.jetbrains.kotlin.idea.js.asJsModule
import org.jetbrains.kotlin.idea.js.jsTestOutputFilePath
import org.jetbrains.kotlin.idea.nodejs.TestElementPath
import org.jetbrains.kotlin.idea.nodejs.getNodeJsEnvironmentVars
import org.jetbrains.kotlin.idea.run.addBuildTask

class ProtractorConfigData(
    override val element: PsiElement,
    override val module: Module,
    override val jsOutputFilePath: String
) : KotlinJSRunConfigurationData

class KotlinProtractorRunConfigurationProducer :
        CompatibleRunConfigurationProducer<KotlinProtractorRunConfiguration>(KotlinProtractorConfigurationType.getInstance()),
        KotlinJSRunConfigurationDataProvider<ProtractorConfigData> {
    override val isForTests: Boolean
        get() = true

    override fun getConfigurationData(
        context: ConfigurationContext
    ): ProtractorConfigData? {
        val module = context.module?.asJsModule() ?: return null
        val element = context.psiLocation ?: return null
        if (!TestElementPath.isModuleAssociatedDir(element, module)) return null
        val testFilePath = module.jsTestOutputFilePath ?: return null
        return ProtractorConfigData(element, module, testFilePath)
    }

    override fun isConfigurationFromCompatibleContext(
        configuration: KotlinProtractorRunConfiguration,
        context: ConfigurationContext
    ): Boolean {
        val configData = getConfigurationData(context) ?: return false
        return configuration.runSettings.testFileSystemDependentPath == FileUtil.toSystemDependentName(configData.jsOutputFilePath)
    }

    override fun setupConfigurationFromCompatibleContext(
        configuration: KotlinProtractorRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val element = context.psiLocation ?: return false
        val configData = getConfigurationData(context) ?: return false
        sourceElement.set(element)
        configuration.runSettings = configuration.runSettings.copy(
                testFilePath = configData.jsOutputFilePath,
                envData = configData.module.getNodeJsEnvironmentVars(true)
        )
        configuration.name = configuration.suggestedName()
        configuration.addBuildTask()
        return true
    }
}