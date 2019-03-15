/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.nodejs.cli

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.jetbrains.nodejs.run.NodeJsRunConfiguration
import com.jetbrains.nodejs.run.NodeJsRunConfigurationType
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.js.KotlinJSRunConfigurationData
import org.jetbrains.kotlin.idea.js.KotlinJSRunConfigurationDataProvider
import org.jetbrains.kotlin.idea.js.asJsModule
import org.jetbrains.kotlin.idea.js.jsProductionOutputFilePath
import org.jetbrains.kotlin.idea.nodejs.TestElementPath
import org.jetbrains.kotlin.idea.nodejs.getNodeJsEnvironmentVars
import org.jetbrains.kotlin.idea.project.languageVersionSettings
import org.jetbrains.kotlin.idea.run.addBuildTask
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

class NodeJsConfigData(
    override val element: PsiElement,
    override val module: Module,
    override val jsOutputFilePath: String
) : KotlinJSRunConfigurationData

private class KotlinNodeJsRunConfigurationProducer :
        RunConfigurationProducer<NodeJsRunConfiguration>(NodeJsRunConfigurationType.getInstance()),
        KotlinJSRunConfigurationDataProvider<NodeJsConfigData> {
    private val ConfigurationContext?.isAcceptable: Boolean
        get() {
            val original = this?.getOriginalConfiguration(null)
            return original == null || original is NodeJsRunConfiguration
        }

    override val isForTests: Boolean
        get() = false

    override fun getConfigurationData(context: ConfigurationContext): NodeJsConfigData? {
        if (!context.isAcceptable) return null
        val element = context.psiLocation ?: return null
        val module = context.module?.asJsModule() ?: return null

        val jsFilePath = module.jsProductionOutputFilePath ?: return null
        val declaration = element.getNonStrictParentOfType<KtNamedDeclaration>()
        if (declaration is KtNamedFunction) {
            val detector = MainFunctionDetector(declaration.languageVersionSettings) { it.resolveToDescriptorIfAny() }
            if (!detector.isMain(declaration, false)) return null
        } else if (!TestElementPath.isModuleAssociatedDir(element, module)) return null
        return NodeJsConfigData(element, module, jsFilePath)
    }

    override fun setupConfigurationFromContext(
        configuration: NodeJsRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val psiElement = sourceElement.get() ?: return false
        val configData = getConfigurationData(context) ?: return false

        if (configuration.workingDirectory.isNullOrBlank()) {
            configuration.workingDirectory = FileUtil.toSystemDependentName(psiElement.project.baseDir.path)
        }
        configuration.inputPath = configData.jsOutputFilePath
        configuration.envs = configData.module.getNodeJsEnvironmentVars(false).envs
        configuration.setGeneratedName()
        configuration.addBuildTask()

        return true
    }

    override fun isConfigurationFromContext(configuration: NodeJsRunConfiguration, context: ConfigurationContext): Boolean {
        val configData = getConfigurationData(context) ?: return false
        return configuration.inputPath == configData.jsOutputFilePath
    }
}
