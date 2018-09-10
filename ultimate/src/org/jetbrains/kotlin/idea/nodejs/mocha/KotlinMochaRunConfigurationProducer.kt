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

package org.jetbrains.kotlin.idea.nodejs.mocha

import com.intellij.execution.RunManager
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
import com.intellij.util.SmartList
import com.intellij.util.containers.SmartHashSet
import com.jetbrains.nodejs.mocha.MochaUtil
import com.jetbrains.nodejs.mocha.execution.*
import org.jetbrains.kotlin.idea.js.KotlinJSRunConfigurationData
import org.jetbrains.kotlin.idea.js.KotlinJSRunConfigurationDataProvider
import org.jetbrains.kotlin.idea.js.asJsModule
import org.jetbrains.kotlin.idea.js.jsTestOutputFilePath
import org.jetbrains.kotlin.idea.nodejs.TestElementInfo
import org.jetbrains.kotlin.idea.nodejs.TestElementPath
import org.jetbrains.kotlin.idea.nodejs.getNodeJsEnvironmentVars
import org.jetbrains.kotlin.idea.run.addBuildTask

private typealias MochaTestElementInfo = TestElementInfo<MochaRunSettings>

class MochaConfigData(
    override val element: PsiElement,
    override val module: Module,
    override val jsOutputFilePath: String,
    val testElementPath: TestElementPath
) : KotlinJSRunConfigurationData

class KotlinMochaRunConfigurationProducer : MochaRunConfigurationProducer(), KotlinJSRunConfigurationDataProvider<MochaConfigData> {
    // Copied from MochaRunConfigurationProducer.collectMochaTestRoots()
    private fun collectMochaTestRoots(project: Project): List<VirtualFile> {
        return RunManager
            .getInstance(project)
            .getConfigurationsList(MochaConfigurationType.getInstance())
            .filterIsInstance<MochaRunConfiguration>()
            .mapNotNullTo(SmartList<VirtualFile>()) { configuration ->
                val settings = configuration.runSettings
                val path = when (settings.testKind) {
                    MochaTestKind.DIRECTORY -> settings.testDirPath
                    MochaTestKind.TEST_FILE,
                    MochaTestKind.SUITE,
                    MochaTestKind.TEST -> settings.testFilePath
                    else -> null
                }
                if (path.isNullOrBlank()) return@mapNotNullTo null
                LocalFileSystem.getInstance().findFileByPath(path!!)
            }
    }

    private fun createTestElementRunInfo(configData: MochaConfigData, originalSettings: MochaRunSettings): MochaTestElementInfo {
        val project = configData.module.project
        val settings = if (originalSettings.workingDir.isBlank()) {
            val workingDir = FileUtil.toSystemDependentName(project.baseDir.path)
            originalSettings.builder().setWorkingDir(workingDir).build()
        } else originalSettings
        val builder = settings.builder()
        builder.setTestFilePath(configData.jsOutputFilePath)
        if (settings.ui.isEmpty()) {
            builder.setUi(MochaUtil.UI_BDD)
        }
        when (configData.testElementPath) {
            is TestElementPath.BySuite -> {
                val (suiteNames, testName) = configData.testElementPath
                if (testName == null) {
                    builder.setTestKind(MochaTestKind.SUITE)
                    builder.setSuiteNames(suiteNames)
                } else {
                    builder.setTestKind(MochaTestKind.TEST)
                    builder.setTestNames(suiteNames + testName)
                }
            }

            is TestElementPath.BySingleFile -> {
                builder.setTestKind(MochaTestKind.TEST_FILE)
            }
        }

        builder.setEnvData(configData.module.getNodeJsEnvironmentVars(true))

        return MochaTestElementInfo(builder.build(), configData.element)
    }

    override fun getConfigurationData(context: ConfigurationContext): MochaConfigData? {
        val module = context.module?.asJsModule() ?: return null
        val element = context.psiLocation ?: return null
        val file = module.moduleFile ?: return null
        val project = module.project

        val testFilePath = module.jsTestOutputFilePath ?: return null
        val testElementPath = TestElementPath.forElement(element, module) ?: return null

        val configData = MochaConfigData(element, module, testFilePath, testElementPath)

        if (context.getOriginalConfiguration(MochaConfigurationType.getInstance()) is MochaRunConfiguration) return configData

        if (isTestRunnerPackageAvailableFor(project, file)) return configData

        val roots = collectMochaTestRoots(project)
        if (roots.isEmpty()) return null

        val dirs = SmartHashSet<VirtualFile>()
        for (root in roots) {
            when {
                root.isDirectory -> dirs.add(root)
                root == file -> return configData
            }
        }
        return if (VfsUtilCore.isUnder(file, dirs)) configData else null
    }

    override val isForTests: Boolean
        get() = true

    override fun isConfigurationFromCompatibleContext(configuration: MochaRunConfiguration, context: ConfigurationContext): Boolean {
        val configData = getConfigurationData(context) ?: return false
        val (thisRunSettings, _) = createTestElementRunInfo(configData, configuration.runSettings)
        val thatRunSettings = configuration.runSettings
        val thisTestKind = thisRunSettings.testKind
        if (thisTestKind != thatRunSettings.testKind) return false
        return when {
            thisTestKind == MochaTestKind.DIRECTORY -> thisRunSettings.testDirPath == thatRunSettings.testDirPath
            thisTestKind == MochaTestKind.PATTERN -> thisRunSettings.testFilePattern == thatRunSettings.testFilePattern
            thisTestKind == MochaTestKind.TEST_FILE -> thisRunSettings.testFilePath == thatRunSettings.testFilePath
            thisTestKind == MochaTestKind.SUITE -> thisRunSettings.testFilePath == thatRunSettings.testFilePath && thisRunSettings.suiteNames == thatRunSettings.suiteNames
            thisTestKind != MochaTestKind.TEST -> false
            else -> thisRunSettings.testFilePath == thatRunSettings.testFilePath && thisRunSettings.testNames == thatRunSettings.testNames
        }
    }

    override fun setupConfigurationFromCompatibleContext(
        configuration: MochaRunConfiguration,
        context: ConfigurationContext,
        sourceElement: Ref<PsiElement>
    ): Boolean {
        val configData = getConfigurationData(context) ?: return false
        val (runSettings, enclosingTestElement) = createTestElementRunInfo(configData, configuration.runSettings)
        if (runSettings.testKind == MochaTestKind.DIRECTORY) return false
        configuration.runSettings = runSettings
        sourceElement.set(enclosingTestElement)
        configuration.setGeneratedName()
        configuration.addBuildTask()
        return true
    }
}