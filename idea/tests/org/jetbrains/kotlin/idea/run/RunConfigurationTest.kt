/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.module.Module
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiManager
import com.intellij.refactoring.RefactoringFactory
import com.intellij.testFramework.MapDataContext
import com.intellij.testFramework.PsiTestUtil
import org.jetbrains.kotlin.checkers.languageVersionSettingsFromText
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.project.setLanguageVersionSettings
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionFqnNameIndex
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase.*
import org.jetbrains.kotlin.idea.test.configureLanguageAndApiVersion
import org.jetbrains.kotlin.idea.util.application.runWriteAction
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.test.JUnit3WithIdeaConfigurationRunner
import org.junit.runner.RunWith
import java.io.File
import java.util.*

private const val RUN_PREFIX = "// RUN:"

@RunWith(JUnit3WithIdeaConfigurationRunner::class)
class RunConfigurationTest : AbstractRunConfigurationTest() {
    fun testMainInTest() {
        val createResult = configureModule(moduleDirPath("module"), getTestProject().baseDir!!)
        configureLanguageAndApiVersion(
            createResult.module.project, createResult.module, LanguageVersionSettingsImpl.DEFAULT.languageVersion.versionString
        )
        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(createResult.module, addJdk(testRootDisposable, ::mockJdk))

        val runConfiguration = createConfigurationFromMain("some.main")
        val javaParameters = getJavaRunParameters(runConfiguration)

        assertTrue(javaParameters.classPath.rootDirs.contains(createResult.srcOutputDir))
        assertTrue(javaParameters.classPath.rootDirs.contains(createResult.testOutputDir))

        fun functionVisitor(function: KtNamedFunction) {
            val file = function.containingKtFile
            val options = function.bodyExpression?.allChildren?.filterIsInstance<PsiComment>()?.map { it.text.trim().replace("//", "").trim() }?.filter { it.isNotBlank() }?.toList() ?: emptyList()
            if (options.isNotEmpty()) {
                val assertIsMain = "yes" in options
                val assertIsNotMain = "no" in options

                val languageVersionSettings = languageVersionSettingsFromText(listOf(file.text))
                createResult.module.setLanguageVersionSettings(languageVersionSettings)
                val isMainFunction =
                    MainFunctionDetector(languageVersionSettings) { it.resolveToDescriptorIfAny() }.isMain(function)

                if (assertIsMain) {
                    assertTrue("$file: The function ${function.fqName?.asString()} should be main", isMainFunction)
                }
                if (assertIsNotMain) {
                    assertFalse("$file: The function ${function.fqName?.asString()} should NOT be main", isMainFunction)
                }

                if (isMainFunction) {
                    createConfigurationFromMain(function.fqName?.asString()!!).checkConfiguration()

                    assertNotNull(
                        "$file: Kotlin configuration producer should produce configuration for ${function.fqName?.asString()}",
                        KotlinRunConfigurationProducer.getEntryPointContainer(function)
                    )
                } else {
                    try {
                        createConfigurationFromMain(function.fqName?.asString()!!).checkConfiguration()
                        fail("$file: configuration for function ${function.fqName?.asString()} at least shouldn't pass checkConfiguration()")
                    } catch (expected: Throwable) {
                    }

                    if (function.containingFile.text.startsWith("// entryPointExists")) {
                        assertNotNull(
                            "$file: Kotlin configuration producer should produce configuration for ${function.fqName?.asString()}",
                            KotlinRunConfigurationProducer.getEntryPointContainer(function)
                        )
                    } else {
                        assertNull(
                            "Kotlin configuration producer shouldn't produce configuration for ${function.fqName?.asString()}",
                            KotlinRunConfigurationProducer.getEntryPointContainer(function)
                        )
                    }
                }
            }
        }

        createResult.srcDir?.children?.filter { it.extension == "kt" }?.forEach {
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

        assertTrue(javaParameters.classPath.rootDirs.contains(dependencyModuleSrcDir))
        assertTrue(javaParameters.classPath.rootDirs.contains(moduleWithDependencySrcDir))
    }

    fun testLongCommandLine() {
        val myModule = configureModule(moduleDirPath("module"), getTestProject().baseDir).module
        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(module, addJdk(testRootDisposable, ::mockJdk))

        ModuleRootModificationUtil.addDependency(myModule, createLibraryWithLongPaths(project))

        val kotlinRunConfiguration = createConfigurationFromMain("some.test.main")
        kotlinRunConfiguration.setModule(myModule)

        val javaParameters = getJavaRunParameters(kotlinRunConfiguration)
        val commandLine = javaParameters.toCommandLine().commandLineString
        assert(commandLine.length > javaParameters.classPath.pathList.joinToString(File.pathSeparator).length) {
            "Wrong command line length: \ncommand line = $commandLine, \nclasspath = ${javaParameters.classPath.pathList.joinToString()}"
        }
    }

    fun testClassesAndObjects() {
        doTest(ConfigLibraryUtil::configureKotlinRuntimeAndSdk)
    }

    fun testInJsModule() {
        doTest(ConfigLibraryUtil::configureKotlinJsRuntimeAndSdk)
    }

    fun testUpdateOnClassRename() {
        val createModuleResult = configureModule(moduleDirPath("module"), getTestProject().baseDir!!)
        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(createModuleResult.module, addJdk(testRootDisposable, ::mockJdk))

        val runConfiguration = createConfigurationFromObject("renameTest.Foo", save = true)

        val obj = KotlinFullClassNameIndex.getInstance().get("renameTest.Foo", getTestProject(), getTestProject().allScope()).single()
        val rename = RefactoringFactory.getInstance(getTestProject()).createRename(obj, "Bar")
        rename.run()

        assertEquals("renameTest.Bar", runConfiguration.MAIN_CLASS_NAME)
    }

    fun testUpdateOnPackageRename() {
        val createModuleResult = configureModule(moduleDirPath("module"), getTestProject().baseDir!!)
        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(createModuleResult.module, addJdk(testRootDisposable, ::mockJdk))

        val runConfiguration = createConfigurationFromObject("renameTest.Foo", save = true)

        val pkg = JavaPsiFacade.getInstance(getTestProject()).findPackage("renameTest")!!
        val rename = RefactoringFactory.getInstance(getTestProject()).createRename(pkg, "afterRenameTest")
        rename.run()

        assertEquals("afterRenameTest.Foo", runConfiguration.MAIN_CLASS_NAME)
    }

    fun testWithModuleForJdk6() {
        checkModuleInfoName(null, addJdk(testRootDisposable, ::mockJdk))
    }

    fun testWithModuleForJdk9() {
        checkModuleInfoName("MAIN", addJdk(testRootDisposable, ::mockJdk9))
    }

    fun testWithModuleForJdk9WithoutModuleInfo() {
        checkModuleInfoName(null, addJdk(testRootDisposable, ::mockJdk9))
    }

    private fun checkModuleInfoName(moduleName: String?, sdk: Sdk) {
        val module = configureModule(moduleDirPath("module"), getTestProject().baseDir!!).module
        ConfigLibraryUtil.configureKotlinRuntimeAndSdk(module, sdk)

        val javaParameters = getJavaRunParameters(createConfigurationFromMain("some.main"))

        assertEquals(moduleName, javaParameters.moduleName)
    }

    private fun doTest(configureRuntime: (Module, Sdk) -> Unit) {
        val baseDir = getTestProject().baseDir!!
        val createModuleResult = configureModule(moduleDirPath("module"), baseDir)
        val srcDir = createModuleResult.srcDir!!

        configureRuntime(createModuleResult.module, addJdk(testRootDisposable, ::mockJdk))

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
            assertEquals(expectedClasses, actualClasses)
        }
        finally {
            ConfigLibraryUtil.unConfigureKotlinRuntimeAndSdk(createModuleResult.module, mockJdk())
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

    override fun getTestDataPath() = getTestDataPathBase() + "/run/"
}
