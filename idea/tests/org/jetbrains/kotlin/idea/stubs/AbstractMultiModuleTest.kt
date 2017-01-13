/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.stubs

import com.intellij.codeInsight.daemon.DaemonAnalyzerTestCase
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.externalSystem.service.project.IdeModifiableModelsProviderImpl
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.testFramework.PsiTestUtil
import com.sampullara.cli.Argument
import org.jetbrains.kotlin.cli.common.arguments.CommonCompilerArguments
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.facet.getOrCreateFacet
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import java.io.File

abstract class AbstractMultiModuleTest : DaemonAnalyzerTestCase() {

    protected open val testPath = PluginTestCaseBase.getTestDataPathBase()

    protected fun module(name: String, hasTestRoot: Boolean = false, useFullJdk: Boolean = false): Module {
        val srcDir = testPath + "${getTestName(true)}/$name"
        val moduleWithSrcRootSet = createModuleFromTestData(srcDir, name, StdModuleTypes.JAVA, true)!!
        if (hasTestRoot) {
            setTestRoot(moduleWithSrcRootSet, name)
        }

        val jdkToUse = if (useFullJdk) PluginTestCaseBase.fullJdk() else PluginTestCaseBase.mockJdk()
        ConfigLibraryUtil.configureSdk(moduleWithSrcRootSet, jdkToUse)

        return moduleWithSrcRootSet
    }

    private fun setTestRoot(module: Module, name: String) {
        val testDir = testPath + "${getTestName(true)}/${name}Test"
        val testRootDirInTestData = File(testDir)
        val testRootDir = createTempDirectory()!!
        FileUtil.copyDir(testRootDirInTestData, testRootDir)
        val testRoot = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(testRootDir)!!
        object : WriteCommandAction.Simple<Unit>(project) {
            override fun run() {
                testRoot.refresh(false, true)
            }
        }.execute().throwException()
        PsiTestUtil.addSourceRoot(module, testRoot, true)
    }

    protected fun Module.addDependency(
            other: Module,
            dependencyScope: DependencyScope = DependencyScope.COMPILE,
            exported: Boolean = false
    ) = ModuleRootModificationUtil.addDependency(this, other, dependencyScope, exported)

    private fun Module.createFacet() {
        val accessToken = WriteAction.start()
        try {
            val modelsProvider = IdeModifiableModelsProviderImpl(project)
            getOrCreateFacet(modelsProvider, true)
            modelsProvider.commit()
        }
        finally {
            accessToken.finish()
        }
    }

    protected fun Module.setPlatformKind(platformKind: TargetPlatformKind<*>) {
        createFacet()
        val facetSettings = KotlinFacetSettingsProvider.getInstance(project).getSettings(this)
        val versionInfo = facetSettings.versionInfo
        versionInfo.targetPlatformKind = platformKind
    }

    protected fun Module.enableMultiPlatform() {
        createFacet()
        val facetSettings = KotlinFacetSettingsProvider.getInstance(project).getSettings(this)
        val compilerInfo = facetSettings.compilerInfo
        val compilerSettings = CompilerSettings()
        compilerSettings.additionalArguments += " -$multiPlatformArg"
        compilerInfo.compilerSettings = compilerSettings
    }

    companion object {
        private val multiPlatformArg = CommonCompilerArguments::multiPlatform.annotations.filterIsInstance<Argument>().single().value
    }

}