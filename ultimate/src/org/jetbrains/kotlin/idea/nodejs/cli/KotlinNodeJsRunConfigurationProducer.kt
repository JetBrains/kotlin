/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.nodejs.cli

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.actions.RunConfigurationProducer
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.jetbrains.nodejs.run.NodeJsRunConfiguration
import com.jetbrains.nodejs.run.NodeJsRunConfigurationType
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.js.jsOrJsImpl
import org.jetbrains.kotlin.idea.js.jsProductionOutputFilePath
import org.jetbrains.kotlin.idea.nodejs.TestElementPath
import org.jetbrains.kotlin.idea.nodejs.getNodeJsEnvironmentVars
import org.jetbrains.kotlin.idea.run.addBuildTask
import org.jetbrains.kotlin.idea.util.module
import org.jetbrains.kotlin.psi.KtNamedDeclaration
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType

private class KotlinNodeJsRunConfigurationProducer :
        RunConfigurationProducer<NodeJsRunConfiguration>(NodeJsRunConfigurationType.getInstance()) {
    private val ConfigurationContext?.isAcceptable: Boolean
        get() {
            val original = this?.getOriginalConfiguration(null)
            return original == null || original is NodeJsRunConfiguration
        }

    override fun setupConfigurationFromContext(
        configuration: NodeJsRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        if (!context.isAcceptable) return false

        val psiElement = sourceElement.get() ?: return false
        val psiFile = psiElement.containingFile ?: return false
        val jsModule = psiFile.module?.jsOrJsImpl() ?: return false
        val project = psiFile.project

        val declaration = psiElement.getNonStrictParentOfType<KtNamedDeclaration>()
        if (declaration is KtNamedFunction) {
            val detector = MainFunctionDetector { it.resolveToDescriptorIfAny() }
            if (!detector.isMain(declaration, false)) return false
        }
        else if (!TestElementPath.isModuleAssociatedDir(psiElement, jsModule)) return false

        val jsFilePath = jsModule.jsProductionOutputFilePath ?: return false

        if (configuration.workingDirectory.isNullOrBlank()) {
            configuration.workingDirectory = FileUtil.toSystemDependentName(project.baseDir.path)
        }
        configuration.inputPath = jsFilePath
        configuration.envs = jsModule.getNodeJsEnvironmentVars(false).envs
        configuration.setGeneratedName()
        configuration.addBuildTask()

        return true
    }

    override fun isConfigurationFromContext(configuration: NodeJsRunConfiguration, context: ConfigurationContext): Boolean {
        if (!context.isAcceptable) return false

        val contextPsi = context.psiLocation ?: return false
        val jsModule = contextPsi.module?.jsOrJsImpl() ?: return false
        val jsFilePath = jsModule.jsProductionOutputFilePath ?: return false
        return configuration.inputPath == jsFilePath
    }
}