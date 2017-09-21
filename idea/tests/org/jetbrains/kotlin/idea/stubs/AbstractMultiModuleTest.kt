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
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.libraries.PersistentLibraryKind
import com.intellij.openapi.roots.ui.configuration.libraryEditor.NewLibraryEditor
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.openapi.vfs.newvfs.impl.VfsRootAccess
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.kotlin.config.CompilerSettings
import org.jetbrains.kotlin.config.KotlinFacetSettingsProvider
import org.jetbrains.kotlin.config.TargetPlatformKind
import org.jetbrains.kotlin.idea.facet.getOrCreateFacet
import org.jetbrains.kotlin.idea.facet.initializeIfNeeded
import org.jetbrains.kotlin.idea.project.PluginJetFilesProvider
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinJdkAndLibraryProjectDescriptor
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.junit.Assert
import java.io.File

abstract class AbstractMultiModuleTest : DaemonAnalyzerTestCase() {

    abstract override fun getTestDataPath(): String

    override fun setUp() {
        super.setUp()
        VfsRootAccess.allowRootAccess(KotlinTestUtils.getHomeDirectory())
    }

    protected fun module(name: String, jdk: TestJdkKind = TestJdkKind.MOCK_JDK, hasTestRoot: Boolean = false): Module {
        val srcDir = testDataPath + "${getTestName(true)}/$name"
        val moduleWithSrcRootSet = createModuleFromTestData(srcDir, name, StdModuleTypes.JAVA, true)!!
        if (hasTestRoot) {
            setTestRoot(moduleWithSrcRootSet, name)
        }

        ConfigLibraryUtil.configureSdk(moduleWithSrcRootSet, PluginTestCaseBase.jdk(jdk))

        return moduleWithSrcRootSet
    }

    private fun setTestRoot(module: Module, name: String) {
        val testDir = testDataPath + "${getTestName(true)}/${name}Test"
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
    ): Module = this.apply { ModuleRootModificationUtil.addDependency(this, other, dependencyScope, exported) }

    protected fun Module.addLibrary(jar: File,
                                    name: String = KotlinJdkAndLibraryProjectDescriptor.LIBRARY_NAME,
                                    kind: PersistentLibraryKind<*>? = null) {
        ConfigLibraryUtil.addLibrary(NewLibraryEditor().apply {
            this.name = name
            addRoot(VfsUtil.getUrlForLibraryRoot(jar), OrderRootType.CLASSES)
        }, this, kind)
    }

    protected fun Module.enableMultiPlatform() {
        createFacet()
        val facetSettings = KotlinFacetSettingsProvider.getInstance(project).getInitializedSettings(this)
        facetSettings.useProjectSettings = false
        facetSettings.compilerSettings = CompilerSettings().apply {
            additionalArguments += " -Xmulti-platform"
        }
    }

    protected fun checkFiles(shouldCheckFile: () -> Boolean = { true }, check: () -> Unit) {
        var atLeastOneFile = false
        PluginJetFilesProvider.allFilesInProject(myProject!!).forEach { file ->
            configureByExistingFile(file.virtualFile!!)
            if (shouldCheckFile()) {
                atLeastOneFile = true
                check()
            }
        }
        Assert.assertTrue(atLeastOneFile)
    }
}

fun Module.createFacet(platformKind: TargetPlatformKind<*>? = null, useProjectSettings: Boolean = true) {
    val accessToken = WriteAction.start()
    try {
        val modelsProvider = IdeModifiableModelsProviderImpl(project)
        getOrCreateFacet(modelsProvider, useProjectSettings).configuration.settings.initializeIfNeeded(
                this,
                modelsProvider.getModifiableRootModel(this),
                platformKind
        )
        modelsProvider.commit()
    }
    finally {
        accessToken.finish()
    }
}
