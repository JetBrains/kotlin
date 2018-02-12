/*
 * Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.nodejs.jest

import com.intellij.execution.actions.ConfigurationContext
import com.intellij.javascript.jest.JestRunConfiguration
import com.intellij.javascript.jest.JestRunConfigurationProducer
import com.intellij.javascript.jest.JestRunSettings
import com.intellij.javascript.jest.scope.JestScopeKind
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiUtilCore
import org.jetbrains.kotlin.idea.js.jsOrJsImpl
import org.jetbrains.kotlin.idea.js.jsTestOutputFilePath
import org.jetbrains.kotlin.idea.nodejs.TestElementInfo
import org.jetbrains.kotlin.idea.nodejs.TestElementPath
import org.jetbrains.kotlin.idea.nodejs.getNodeJsEnvironmentVars
import org.jetbrains.kotlin.idea.util.projectStructure.module

private typealias JestTestElementInfo = TestElementInfo<JestRunSettings>

class KotlinJestRunConfigurationProducer : JestRunConfigurationProducer() {
    private fun createTestElementRunInfo(element: PsiElement, originalSettings: JestRunSettings): JestTestElementInfo? {
        val module = element.module?.jsOrJsImpl() ?: return null
        val project = module.project
        val testFilePath = module.jsTestOutputFilePath ?: return null
        val settings = if (originalSettings.workingDirSystemDependentPath.isBlank()) {
            val workingDir = FileUtil.toSystemDependentName(project.baseDir.path)
            originalSettings.toBuilder().setWorkingDir(workingDir).build()
        } else originalSettings
        val testElementPath = TestElementPath.forElement(element, module) ?: return null
        val builder = settings.toBuilder()
        builder.setTestFilePath(testFilePath)
        when (testElementPath) {
            is TestElementPath.BySuite -> {
                val (suiteNames, testName) = testElementPath
                if (testName == null) {
                    builder.setScopeKind(JestScopeKind.SUITE)
                    builder.setTestNames(suiteNames)
                } else {
                    builder.setScopeKind(JestScopeKind.TEST)
                    builder.setTestNames(suiteNames + testName)
                }
            }

            is TestElementPath.BySingleFile -> {
                builder.setScopeKind(JestScopeKind.TEST_FILE)
            }
        }
        builder.setEnvData(module.getNodeJsEnvironmentVars())

        return JestTestElementInfo(builder.build(), element)
    }

    override fun isConfigurationFromCompatibleContext(configuration: JestRunConfiguration, context: ConfigurationContext): Boolean {
        val element = context.psiLocation ?: return false
        val (thisRunSettings, _) = createTestElementRunInfo(element, configuration.runSettings) ?: return false
        val thatRunSettings = configuration.runSettings

        if (thisRunSettings.configFileSystemDependentPath != thatRunSettings.configFileSystemDependentPath) return false
        if (thatRunSettings.scopeKind != thisRunSettings.scopeKind) return false
        return when (thisRunSettings.scopeKind) {
            JestScopeKind.ALL -> true
            JestScopeKind.TEST_FILE -> thisRunSettings.testFileSystemDependentPath == thatRunSettings.testFileSystemDependentPath
            JestScopeKind.SUITE, JestScopeKind.TEST ->
                thisRunSettings.testFileSystemDependentPath == thatRunSettings.testFileSystemDependentPath &&
                        thisRunSettings.testNames == thatRunSettings.testNames
            else -> false
        }
    }

    override fun setupConfigurationFromCompatibleContext(
        configuration: JestRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val element = context.psiLocation ?: return false
        val module = element.module
        val jsModule = module?.jsOrJsImpl() ?: return false
        val file = if (jsModule != module) {
            jsModule.moduleFile
        } else {
            PsiUtilCore.getVirtualFile(element)
        } ?: return false
        val project = module.project

        if (!isTestRunnerPackageAvailableFor(project, file)) return false

        val (runSettings, enclosingTestElement) = createTestElementRunInfo(element, configuration.runSettings) ?: return false
        configuration.runSettings = runSettings
        sourceElement.set(enclosingTestElement)
        configuration.setGeneratedName()
        return true
    }
}