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
import com.intellij.openapi.module.Module
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.idea.js.KotlinJSRunConfigurationData
import org.jetbrains.kotlin.idea.js.KotlinJSRunConfigurationDataProvider
import org.jetbrains.kotlin.idea.js.asJsModule
import org.jetbrains.kotlin.idea.js.jsTestOutputFilePath
import org.jetbrains.kotlin.idea.nodejs.TestElementInfo
import org.jetbrains.kotlin.idea.nodejs.TestElementPath
import org.jetbrains.kotlin.idea.nodejs.getNodeJsEnvironmentVars
import org.jetbrains.kotlin.idea.run.addBuildTask

typealias JestTestElementInfo = TestElementInfo<JestRunSettings>

class JestConfigData(
    override val element: PsiElement,
    override val module: Module,
    override val jsOutputFilePath: String,
    val testElementPath: TestElementPath
) : KotlinJSRunConfigurationData

class KotlinJestRunConfigurationProducer :
        JestRunConfigurationProducer(),
        KotlinJSRunConfigurationDataProvider<JestConfigData> {
    private fun createTestElementRunInfo(
        configData: JestConfigData,
        originalSettings: JestRunSettings
    ): JestTestElementInfo {
        val project = configData.module.project

        val settings = if (originalSettings.workingDirSystemDependentPath.isBlank()) {
            val workingDir = FileUtil.toSystemDependentName(project.baseDir.path)
            originalSettings.toBuilder().setWorkingDir(workingDir).build()
        } else originalSettings

        val builder = settings.toBuilder()
        builder.setTestFilePath(configData.jsOutputFilePath)
        when (configData.testElementPath) {
            is TestElementPath.BySuite -> {
                val (suiteNames, testName) = configData.testElementPath
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
        builder.setEnvData(configData.module.getNodeJsEnvironmentVars(true))

        return JestTestElementInfo(builder.build(), configData.element)
    }

    override val isForTests: Boolean
        get() = true

    override fun getConfigurationData(
        context: ConfigurationContext
    ): JestConfigData? {
        val element = context.psiLocation ?: return null
        val module = context.module?.asJsModule() ?: return null

        val file = module.moduleFile

        if (!isTestRunnerPackageAvailableFor(module.project, file)) return null

        val testFilePath = module.jsTestOutputFilePath ?: return null
        val testElementPath = TestElementPath.forElement(element, module) ?: return null

        return JestConfigData(element, module, testFilePath, testElementPath)
    }

    override fun isConfigurationFromCompatibleContext(configuration: JestRunConfiguration, context: ConfigurationContext): Boolean {
        val configData = getConfigurationData(context) ?: return false
        val (thisRunSettings, _) = createTestElementRunInfo(configData, configuration.runSettings)
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
        val configData = getConfigurationData(context) ?: return false
        val (runSettings, enclosingTestElement) = createTestElementRunInfo(configData, configuration.runSettings)
        configuration.runSettings = runSettings
        sourceElement.set(enclosingTestElement)
        configuration.setGeneratedName()
        configuration.addBuildTask()
        return true
    }
}