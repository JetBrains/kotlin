/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.run

import com.intellij.codeInsight.CodeInsightTestCase
import com.intellij.execution.Executor
import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.RunManagerEx
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.execution.configurations.*
import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ExecutionEnvironmentBuilder
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.*
import com.intellij.refactoring.RefactoringFactory
import com.intellij.testFramework.MapDataContext
import com.intellij.testFramework.PlatformTestCase
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.stubindex.JetFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.JetTopLevelFunctionFqnNameIndex
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil.configureKotlinJsRuntimeAndSdk
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil.configureKotlinRuntimeAndSdk
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.junit.Assert
import java.io.File
import java.util.ArrayList

private val RUN_PREFIX = "// RUN:"

class RunConfigurationTest: CodeInsightTestCase() {
    fun getTestProject() = myProject!!
    override fun getModule() = myModule!!

    fun testMainInTest() {
        val createResult = configureModule(moduleDirPath("module"), getTestProject().getBaseDir()!!)
        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(createResult.module, PluginTestCaseBase.mockJdk())

        val runConfiguration = createConfigurationFromMain("some.main")
        val javaParameters = getJavaRunParameters(runConfiguration)

        Assert.assertTrue(javaParameters.getClassPath().getRootDirs().contains(createResult.srcOutputDir))
        Assert.assertTrue(javaParameters.getClassPath().getRootDirs().contains(createResult.testOutputDir))

        fun functionVisitor(function: JetNamedFunction) {
            val options = function.bodyExpression?.allChildren?.filterIsInstance<PsiComment>()?.map { it.text.trim().replace("//", "").trim() }?.filter { it.isNotBlank() }?.toList() ?: emptyList()
            if (options.isNotEmpty()) {
                val assertIsMain = "yes" in options
                val assertIsNotMain = "no" in options

                val bindingContext = function.analyze(BodyResolveMode.FULL)
                val isMainFunction = MainFunctionDetector(bindingContext).isMain(function)

                if (assertIsMain) {
                    Assert.assertTrue("The function ${function.fqName?.asString()} should be main", isMainFunction)
                }
                if (assertIsNotMain) {
                    Assert.assertFalse("The function ${function.fqName?.asString()} should NOT be main", isMainFunction)
                }

                if (isMainFunction) {
                    createConfigurationFromMain(function.fqName?.asString()!!).checkConfiguration()

                    Assert.assertNotNull("Kotlin configuration producer should produce configuration for ${function.fqName?.asString()}",
                                      KotlinRunConfigurationProducer.getEntryPointContainer(function))
                } else {
                    try {
                        createConfigurationFromMain(function.fqName?.asString()!!).checkConfiguration()
                        Assert.fail("configuration for function ${function.fqName?.asString()} at least shouldn't pass checkConfiguration()")
                    } catch (expected: Throwable) {
                    }

                    Assert.assertNull("Kotlin configuration producer shouldN'T produce configuration for ${function.fqName?.asString()}",
                                      KotlinRunConfigurationProducer.getEntryPointContainer(function))
                }
            }
        }

        createResult.srcDir.children.filter { it.extension == "kt" }.forEach {
            val psiFile = PsiManager.getInstance(createResult.module.project).findFile(it)
            if (psiFile is JetFile) {
                psiFile.acceptChildren(object : JetVisitorVoid() {
                    override fun visitNamedFunction(function: JetNamedFunction) {
                        functionVisitor(function)
                    }
                })
            }
        }
    }

    fun testDependencyModuleClasspath() {
        val dependencyModuleSrcDir = configureModule(moduleDirPath("module"), getTestProject().getBaseDir()!!).srcOutputDir

        val moduleWithDependencyDir = runWriteAction { getTestProject().getBaseDir()!!.createChildDirectory(this, "moduleWithDependency") }

        val moduleWithDependency = createModule("moduleWithDependency")
        ModuleRootModificationUtil.setModuleSdk(moduleWithDependency, getTestProjectJdk())

        val moduleWithDependencySrcDir = configureModule(
                moduleDirPath("moduleWithDependency"), moduleWithDependencyDir, configModule = moduleWithDependency).srcOutputDir

        ModuleRootModificationUtil.addDependency(moduleWithDependency, getModule())

        val jetRunConfiguration = createConfigurationFromMain("some.test.main")
        jetRunConfiguration.setModule(moduleWithDependency)

        val javaParameters = getJavaRunParameters(jetRunConfiguration)

        Assert.assertTrue(javaParameters.getClassPath().getRootDirs().contains(dependencyModuleSrcDir))
        Assert.assertTrue(javaParameters.getClassPath().getRootDirs().contains(moduleWithDependencySrcDir))
    }

    fun testClassesAndObjects() {
        doTest(ConfigLibraryUtil::configureKotlinRuntimeAndSdk)
    }

    fun testInJsModule() {
        doTest(ConfigLibraryUtil::configureKotlinJsRuntimeAndSdk)
    }

    fun testUpdateOnClassRename() {
        val createModuleResult = configureModule(moduleDirPath("module"), getTestProject().getBaseDir()!!)
        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(createModuleResult.module, PluginTestCaseBase.mockJdk())

        val runConfiguration = createConfigurationFromObject("renameTest.Foo", save = true)

        val obj = JetFullClassNameIndex.getInstance().get("renameTest.Foo", getTestProject(), getTestProject().allScope()).single()
        val rename = RefactoringFactory.getInstance(getTestProject()).createRename(obj, "Bar")
        rename.run()

        Assert.assertEquals("renameTest.Bar", runConfiguration.MAIN_CLASS_NAME)
    }

    fun testUpdateOnPackageRename() {
        val createModuleResult = configureModule(moduleDirPath("module"), getTestProject().getBaseDir()!!)
        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(createModuleResult.module, PluginTestCaseBase.mockJdk())

        val runConfiguration = createConfigurationFromObject("renameTest.Foo", save = true)

        val pkg = JavaPsiFacade.getInstance(getTestProject()).findPackage("renameTest")
        val rename = RefactoringFactory.getInstance(getTestProject()).createRename(pkg, "afterRenameTest")
        rename.run()

        Assert.assertEquals("afterRenameTest.Foo", runConfiguration.MAIN_CLASS_NAME)
    }

    private fun doTest(configureRuntime: (Module, Sdk) -> Unit) {
        val baseDir = getTestProject().getBaseDir()!!
        val createModuleResult = configureModule(moduleDirPath("module"), baseDir)
        val srcDir = createModuleResult.srcDir

        configureRuntime(createModuleResult.module, PluginTestCaseBase.mockJdk())

        try {
            val expectedClasses = ArrayList<String>()
            val actualClasses = ArrayList<String>()

            val testFile = PsiManager.getInstance(getTestProject()).findFile(srcDir.findFileByRelativePath("test.kt")!!)!!
            testFile.accept(
                    object : JetTreeVisitorVoid() {
                        override fun visitComment(comment: PsiComment) {
                            val declaration = comment.getStrictParentOfType<JetNamedDeclaration>()!!
                            val text = comment.getText() ?: return
                            if (!text.startsWith(RUN_PREFIX)) return

                            val expectedClass = text.substring(RUN_PREFIX.length()).trim()
                            if (expectedClass.isNotEmpty()) expectedClasses.add(expectedClass)

                            val dataContext = MapDataContext()
                            dataContext.put(Location.DATA_KEY, PsiLocation(getTestProject(), declaration))
                            val context = ConfigurationContext.getFromContext(dataContext)
                            val actualClass = (context?.getConfiguration()?.getConfiguration() as? JetRunConfiguration)?.getRunClass()
                            if (actualClass != null) {
                                actualClasses.add(actualClass)
                            }
                        }
                    }
            )
            Assert.assertEquals(expectedClasses, actualClasses)
        }
        finally {
            ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(createModuleResult.module, PluginTestCaseBase.mockJdk())
        }
    }

    private fun createConfigurationFromMain(mainFqn: String): JetRunConfiguration {
        val mainFunction = JetTopLevelFunctionFqnNameIndex.getInstance().get(mainFqn, getTestProject(), getTestProject().allScope()).first()

        return createConfigurationFromElement(mainFunction)
    }

    private fun createConfigurationFromObject(objectFqn: String, save: Boolean = false): JetRunConfiguration {
        val obj = JetFullClassNameIndex.getInstance().get(objectFqn, getTestProject(), getTestProject().allScope()).single()
        val mainFunction = obj.getDeclarations().single { it is JetFunction && it.getName() == "main" }
        return createConfigurationFromElement(mainFunction, save)
    }

    private fun createConfigurationFromElement(element: PsiElement?, save: Boolean = false): JetRunConfiguration {
        val dataContext = MapDataContext()
        dataContext.put(Location.DATA_KEY, PsiLocation(getTestProject(), element))

        val runnerAndConfigurationSettings = ConfigurationContext.getFromContext(dataContext)!!.getConfiguration()
        if (save) {
            RunManagerEx.getInstanceEx(myProject).setTemporaryConfiguration(runnerAndConfigurationSettings)
        }
        return runnerAndConfigurationSettings!!.getConfiguration() as JetRunConfiguration
    }

    private fun configureModule(moduleDir: String, outputParentDir: VirtualFile, configModule: Module = getModule()): CreateModuleResult {
        val srcPath = moduleDir + "/src"
        val srcDir = PsiTestUtil.createTestProjectStructure(getProject(), configModule, srcPath, PlatformTestCase.myFilesToDelete, true)

        val testPath = moduleDir + "/test"
        if (File(testPath).exists()) {
            val testDir = PsiTestUtil.createTestProjectStructure(getProject(), configModule, testPath, PlatformTestCase.myFilesToDelete, false)
            PsiTestUtil.addSourceRoot(getModule(), testDir, true)
        }

        val (srcOutDir, testOutDir) = runWriteAction {
            val outDir = outputParentDir.createChildDirectory(this, "out")
            val srcOutDir = outDir.createChildDirectory(this, "production")
            val testOutDir = outDir.createChildDirectory(this, "test")

            PsiTestUtil.setCompilerOutputPath(configModule, srcOutDir.getUrl(), false)
            PsiTestUtil.setCompilerOutputPath(configModule, testOutDir.getUrl(), true)

            Pair(srcOutDir, testOutDir)
        }

        PsiDocumentManager.getInstance(getTestProject()).commitAllDocuments()

        return CreateModuleResult(configModule, srcDir, srcOutDir, testOutDir)
    }

    private fun moduleDirPath(moduleName: String) = "${getTestDataPath()}${getTestName(false)}/$moduleName"

    private fun getJavaRunParameters(configuration: RunConfiguration): JavaParameters {
        val state = configuration.getState(MockExecutor, ExecutionEnvironmentBuilder.create(myProject!!, MockExecutor, MockProfile).build())

        Assert.assertNotNull(state)
        Assert.assertTrue(state is JavaCommandLine)

        configuration.checkConfiguration()
        return (state as JavaCommandLine).getJavaParameters()!!
    }

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/run/"
    override fun getTestProjectJdk() = PluginTestCaseBase.mockJdk()

    private class CreateModuleResult(
            val module: Module,
            val srcDir: VirtualFile,
            val srcOutputDir: VirtualFile,
            val testOutputDir: VirtualFile
    )

    private object MockExecutor : DefaultRunExecutor() {
        override fun getId() = DefaultRunExecutor.EXECUTOR_ID
    }

    private object MockProfile : RunProfile {
        override fun getState(executor: Executor, env: ExecutionEnvironment) = null
        override fun getIcon() = null
        override fun getName() = null
    }
}
