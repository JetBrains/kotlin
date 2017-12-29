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

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.refactoring.RefactoringFactory
import com.intellij.testFramework.MapDataContext
import com.intellij.testFramework.PlatformTestCase
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.idea.caches.resolve.analyze
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionFqnNameIndex
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.KotlinCodeInsightTestCase
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode
import org.junit.Assert
import java.io.File
import java.util.*

private val RUN_PREFIX = "// RUN:"

class RunConfigurationTest: KotlinCodeInsightTestCase() {
    fun getTestProject() = myProject!!
    override fun getModule() = myModule!!

    fun testMainInTest() {
        val createResult = configureModule(moduleDirPath("module"), getTestProject().baseDir!!)
        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(createResult.module, PluginTestCaseBase.mockJdk())

        val runConfiguration = createConfigurationFromMain("some.main")
        val javaParameters = getJavaRunParameters(runConfiguration)

        Assert.assertTrue(javaParameters.classPath.rootDirs.contains(createResult.srcOutputDir))
        Assert.assertTrue(javaParameters.classPath.rootDirs.contains(createResult.testOutputDir))

        fun functionVisitor(function: KtNamedFunction) {
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
            if (psiFile is KtFile) {
                psiFile.acceptChildren(object : KtVisitorVoid() {
                    override fun visitNamedFunction(function: KtNamedFunction) {
                        functionVisitor(function)
                    }
                })
            }
        }
    }

    fun testDependencyModuleClasspath() {
        val dependencyModuleSrcDir = configureModule(moduleDirPath("module"), getTestProject().baseDir!!).srcOutputDir

        val moduleWithDependencyDir = runWriteAction { getTestProject().baseDir!!.createChildDirectory(this, "moduleWithDependency") }

        val moduleWithDependency = createModule("moduleWithDependency")
        ModuleRootModificationUtil.setModuleSdk(moduleWithDependency, testProjectJdk)

        val moduleWithDependencySrcDir = configureModule(
                moduleDirPath("moduleWithDependency"), moduleWithDependencyDir, configModule = moduleWithDependency).srcOutputDir

        ModuleRootModificationUtil.addDependency(moduleWithDependency, module)

        val kotlinRunConfiguration = createConfigurationFromMain("some.test.main")
        kotlinRunConfiguration.setModule(moduleWithDependency)

        val javaParameters = getJavaRunParameters(kotlinRunConfiguration)

        Assert.assertTrue(javaParameters.classPath.rootDirs.contains(dependencyModuleSrcDir))
        Assert.assertTrue(javaParameters.classPath.rootDirs.contains(moduleWithDependencySrcDir))
    }

    fun testClassesAndObjects() {
        doTest(ConfigLibraryUtil::configureKotlinRuntimeAndSdk)
    }

    fun testInJsModule() {
        doTest(ConfigLibraryUtil::configureKotlinJsRuntimeAndSdk)
    }

    fun testUpdateOnClassRename() {
        val createModuleResult = configureModule(moduleDirPath("module"), getTestProject().baseDir!!)
        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(createModuleResult.module, PluginTestCaseBase.mockJdk())

        val runConfiguration = createConfigurationFromObject("renameTest.Foo", save = true)

        val obj = KotlinFullClassNameIndex.getInstance().get("renameTest.Foo", getTestProject(), getTestProject().allScope()).single()
        val rename = RefactoringFactory.getInstance(getTestProject()).createRename(obj, "Bar")
        rename.run()

        Assert.assertEquals("renameTest.Bar", runConfiguration.MAIN_CLASS_NAME)
    }

    fun testUpdateOnPackageRename() {
        val createModuleResult = configureModule(moduleDirPath("module"), getTestProject().baseDir!!)
        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(createModuleResult.module, PluginTestCaseBase.mockJdk())

        val runConfiguration = createConfigurationFromObject("renameTest.Foo", save = true)

        val pkg = JavaPsiFacade.getInstance(getTestProject()).findPackage("renameTest")!!
        val rename = RefactoringFactory.getInstance(getTestProject()).createRename(pkg, "afterRenameTest")
        rename.run()

        Assert.assertEquals("afterRenameTest.Foo", runConfiguration.MAIN_CLASS_NAME)
    }

    fun testWithModuleForJdk6() {
        checkModuleInfoName(null, PluginTestCaseBase.mockJdk())
    }

    fun testWithModuleForJdk9() {
        checkModuleInfoName("MAIN", PluginTestCaseBase.mockJdk9())
    }

    fun testWithModuleForJdk9WithoutModuleInfo() {
        checkModuleInfoName(null, PluginTestCaseBase.mockJdk9())
    }

    private fun checkModuleInfoName(moduleName: String?, sdk: Sdk) {
        val module = configureModule(moduleDirPath("module"), getTestProject().baseDir!!).module
        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(module, sdk)

        val javaParameters = getJavaRunParameters(createConfigurationFromMain("some.main"))

        Assert.assertEquals(moduleName, javaParameters.moduleName)
    }

    private fun doTest(configureRuntime: (Module, Sdk) -> Unit) {
        val baseDir = getTestProject().baseDir!!
        val createModuleResult = configureModule(moduleDirPath("module"), baseDir)
        val srcDir = createModuleResult.srcDir

        configureRuntime(createModuleResult.module, PluginTestCaseBase.mockJdk())

        try {
            val expectedClasses = ArrayList<String>()
            val actualClasses = ArrayList<String>()

            val testFile = PsiManager.getInstance(getTestProject()).findFile(srcDir.findFileByRelativePath("test.kt")!!)!!
            testFile.accept(
                    object : KtTreeVisitorVoid() {
                        override fun visitComment(comment: PsiComment) {
                            val declaration = comment.getStrictParentOfType<KtNamedDeclaration>()!!
                            val text = comment.text ?: return
                            if (!text.startsWith(RUN_PREFIX)) return

                            val expectedClass = text.substring(RUN_PREFIX.length).trim()
                            if (expectedClass.isNotEmpty()) expectedClasses.add(expectedClass)

                            val dataContext = MapDataContext()
                            dataContext.put(Location.DATA_KEY, PsiLocation(getTestProject(), declaration))
                            val context = ConfigurationContext.getFromContext(dataContext)
                            val actualClass = (context.configuration?.configuration as? KotlinRunConfiguration)?.runClass
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

    private fun createConfigurationFromMain(mainFqn: String): KotlinRunConfiguration {
        val mainFunction = KotlinTopLevelFunctionFqnNameIndex.getInstance().get(mainFqn, getTestProject(), getTestProject().allScope()).first()

        return createConfigurationFromElement(mainFunction) as KotlinRunConfiguration
    }

    private fun createConfigurationFromObject(objectFqn: String, save: Boolean = false): KotlinRunConfiguration {
        val obj = KotlinFullClassNameIndex.getInstance().get(objectFqn, getTestProject(), getTestProject().allScope()).single()
        val mainFunction = obj.declarations.single { it is KtFunction && it.getName() == "main" }
        return createConfigurationFromElement(mainFunction, save) as KotlinRunConfiguration
    }

    private fun configureModule(moduleDir: String, outputParentDir: VirtualFile, configModule: Module = module): CreateModuleResult {
        val srcPath = moduleDir + "/src"
        val srcDir = PsiTestUtil.createTestProjectStructure(project, configModule, srcPath, PlatformTestCase.myFilesToDelete, true)

        val testPath = moduleDir + "/test"
        if (File(testPath).exists()) {
            val testDir = PsiTestUtil.createTestProjectStructure(project, configModule, testPath, PlatformTestCase.myFilesToDelete, false)
            PsiTestUtil.addSourceRoot(module, testDir, true)
        }

        val (srcOutDir, testOutDir) = runWriteAction {
            val outDir = outputParentDir.createChildDirectory(this, "out")
            val srcOutDir = outDir.createChildDirectory(this, "production")
            val testOutDir = outDir.createChildDirectory(this, "test")

            PsiTestUtil.setCompilerOutputPath(configModule, srcOutDir.url, false)
            PsiTestUtil.setCompilerOutputPath(configModule, testOutDir.url, true)

            Pair(srcOutDir, testOutDir)
        }

        PsiDocumentManager.getInstance(getTestProject()).commitAllDocuments()

        return CreateModuleResult(configModule, srcDir, srcOutDir, testOutDir)
    }

    private fun moduleDirPath(moduleName: String) = "${testDataPath}${getTestName(false)}/$moduleName"

    override fun getTestDataPath() = PluginTestCaseBase.getTestDataPathBase() + "/run/"
    override fun getTestProjectJdk() = PluginTestCaseBase.mockJdk()

    private class CreateModuleResult(
            val module: Module,
            val srcDir: VirtualFile,
            val srcOutputDir: VirtualFile,
            val testOutputDir: VirtualFile
    )
}
