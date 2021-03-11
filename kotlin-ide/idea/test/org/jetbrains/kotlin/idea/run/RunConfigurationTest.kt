/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.run

import com.intellij.execution.Location
import com.intellij.execution.PsiLocation
import com.intellij.execution.actions.ConfigurationContext
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiManager
import com.intellij.refactoring.RefactoringFactory
import com.intellij.testFramework.IdeaTestUtil
import com.intellij.testFramework.MapDataContext
import org.jetbrains.kotlin.checkers.languageVersionSettingsFromText
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.config.LanguageVersionSettingsImpl
import org.jetbrains.kotlin.idea.MainFunctionDetector
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptorIfAny
import org.jetbrains.kotlin.idea.project.withLanguageVersionSettings
import org.jetbrains.kotlin.idea.search.allScope
import org.jetbrains.kotlin.idea.stubindex.KotlinFullClassNameIndex
import org.jetbrains.kotlin.idea.stubindex.KotlinTopLevelFunctionFqnNameIndex
import org.jetbrains.kotlin.idea.test.IDEA_TEST_DATA_DIR
import org.jetbrains.kotlin.idea.test.withCustomLanguageAndApiVersion
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.allChildren
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.junit.internal.runners.JUnit38ClassRunner
import org.junit.runner.RunWith
import java.io.File
import java.util.*

private const val RUN_PREFIX = "// RUN:"

@RunWith(JUnit38ClassRunner::class)
class RunConfigurationTest : AbstractRunConfigurationTest() {
    fun testMainInTest() {
        configureProject()
        val configuredModule = defaultConfiguredModule

        val languageVersion = LanguageVersionSettingsImpl.DEFAULT.languageVersion
        withCustomLanguageAndApiVersion(project, module, languageVersion.versionString, apiVersion = null) {
            val runConfiguration = createConfigurationFromMain(project, "some.main")
            val javaParameters = getJavaRunParameters(runConfiguration)

            assertTrue(javaParameters.classPath.rootDirs.contains(configuredModule.srcOutputDir))
            assertTrue(javaParameters.classPath.rootDirs.contains(configuredModule.testOutputDir))

            val ktFiles = configuredModule.srcDir?.children?.filter { it.extension == "kt" }.orEmpty()
            val psiManager = PsiManager.getInstance(project)

            for (ktFile in ktFiles) {
                val psiFile = psiManager.findFile(ktFile) as? KtFile ?: continue
                val languageVersionSettings = languageVersionSettingsFromText(listOf(psiFile.text))

                module.withLanguageVersionSettings(languageVersionSettings) {
                    psiFile.acceptChildren(
                        object : KtVisitorVoid() {
                            override fun visitNamedFunction(function: KtNamedFunction) {
                                functionVisitor(languageVersionSettings, function)
                            }
                        }
                    )
                }
            }
        }
    }

    fun testDependencyModuleClasspath() {
        configureProject()
        val configuredModule = defaultConfiguredModule
        val configuredModuleWithDependency = getConfiguredModule("moduleWithDependency")

        ModuleRootModificationUtil.addDependency(configuredModuleWithDependency.module, configuredModule.module)

        val kotlinRunConfiguration = createConfigurationFromMain(project, "some.test.main")
        kotlinRunConfiguration.setModule(configuredModuleWithDependency.module)

        val javaParameters = getJavaRunParameters(kotlinRunConfiguration)

        assertTrue(javaParameters.classPath.rootDirs.contains(configuredModule.srcOutputDir))
        assertTrue(javaParameters.classPath.rootDirs.contains(configuredModuleWithDependency.srcOutputDir))
    }

    fun testLongCommandLine() {
        configureProject()
        ModuleRootModificationUtil.addDependency(module, createLibraryWithLongPaths(project))

        val kotlinRunConfiguration = createConfigurationFromMain(project, "some.test.main")
        kotlinRunConfiguration.setModule(module)

        val javaParameters = getJavaRunParameters(kotlinRunConfiguration)
        val commandLine = javaParameters.toCommandLine().commandLineString
        assert(commandLine.length > javaParameters.classPath.pathList.joinToString(File.pathSeparator).length) {
            "Wrong command line length: \ncommand line = $commandLine, \nclasspath = ${javaParameters.classPath.pathList.joinToString()}"
        }
    }

    fun testClassesAndObjects() = checkClasses()

    fun testInJsModule() = checkClasses(Platform.JavaScript)

    fun testUpdateOnClassRename() {
        configureProject()

        val runConfiguration = createConfigurationFromObject("renameTest.Foo")

        val obj = KotlinFullClassNameIndex.getInstance().get("renameTest.Foo", project, project.allScope()).single()
        val rename = RefactoringFactory.getInstance(project).createRename(obj, "Bar")
        rename.run()

        assertEquals("renameTest.Bar", runConfiguration.MAIN_CLASS_NAME)
    }

    fun testUpdateOnPackageRename() {
        configureProject()

        val runConfiguration = createConfigurationFromObject("renameTest.Foo")

        val pkg = JavaPsiFacade.getInstance(project).findPackage("renameTest") ?: error("Package 'renameTest' not found")
        val rename = RefactoringFactory.getInstance(project).createRename(pkg, "afterRenameTest")
        rename.run()

        assertEquals("afterRenameTest.Foo", runConfiguration.MAIN_CLASS_NAME)
    }

    fun testWithModuleForJdk6() {
        checkModuleInfoName(null, Platform.Jvm(IdeaTestUtil.getMockJdk16()))
    }

    fun testWithModuleForJdk9() {
        checkModuleInfoName("MAIN", Platform.Jvm(IdeaTestUtil.getMockJdk9()))
    }

    fun testWithModuleForJdk9WithoutModuleInfo() {
        checkModuleInfoName(null, Platform.Jvm(IdeaTestUtil.getMockJdk9()))
    }

    private fun checkModuleInfoName(moduleName: String?, platform: Platform) {
        configureProject(platform)

        val javaParameters = getJavaRunParameters(createConfigurationFromMain(project, "some.main"))
        assertEquals(moduleName, javaParameters.moduleName)
    }

    private fun checkClasses(platform: Platform = Platform.Jvm()) {
        configureProject(platform)
        val srcDir = defaultConfiguredModule.srcDir ?: error("Module doesn't have a production source set")

        val expectedClasses = ArrayList<String>()
        val actualClasses = ArrayList<String>()

        val fileName = "test.kt"
        val testKtVirtualFile = srcDir.findFileByRelativePath(fileName) ?: error("Can't find VirtualFile for $fileName")
        val testFile = PsiManager.getInstance(project).findFile(testKtVirtualFile) ?: error("Can't find PSI for $fileName")

        val visitor = object : KtTreeVisitorVoid() {
            override fun visitComment(comment: PsiComment) {
                val declaration = comment.getStrictParentOfType<KtNamedDeclaration>()!!
                val text = comment.text ?: return
                if (!text.startsWith(RUN_PREFIX)) return

                val expectedClass = text.substring(RUN_PREFIX.length).trim()
                if (expectedClass.isNotEmpty()) expectedClasses.add(expectedClass)

                val dataContext = MapDataContext()
                dataContext.put(Location.DATA_KEY, PsiLocation(project, declaration))
                val context = ConfigurationContext.getFromContext(dataContext)
                val actualClass = (context.configuration?.configuration as? KotlinRunConfiguration)?.runClass
                if (actualClass != null) {
                    actualClasses.add(actualClass)
                }
            }
        }

        testFile.accept(visitor)
        assertEquals(expectedClasses, actualClasses)
    }

    private fun createConfigurationFromObject(@Suppress("SameParameterValue") objectFqn: String): KotlinRunConfiguration {
        val obj = KotlinFullClassNameIndex.getInstance().get(objectFqn, project, project.allScope()).single()
        val mainFunction = obj.declarations.single { it is KtFunction && it.getName() == "main" }
        return createConfigurationFromElement(mainFunction, true) as KotlinRunConfiguration
    }

    companion object {
        private fun functionVisitor(fileLanguageSettings: LanguageVersionSettings, function: KtNamedFunction) {
            val project = function.project
            val file = function.containingKtFile
            val options = function.bodyExpression?.allChildren?.filterIsInstance<PsiComment>()
                ?.map { it.text.trim().replace("//", "").trim() }
                ?.filter { it.isNotBlank() }?.toList() ?: emptyList()

            if (options.isNotEmpty()) {
                val assertIsMain = "yes" in options
                val assertIsNotMain = "no" in options

                val isMainFunction = MainFunctionDetector(fileLanguageSettings) { it.resolveToDescriptorIfAny() }.isMain(function)

                if (assertIsMain) {
                    assertTrue("$file: The function ${function.fqName?.asString()} should be main", isMainFunction)
                }
                if (assertIsNotMain) {
                    assertFalse("$file: The function ${function.fqName?.asString()} should NOT be main", isMainFunction)
                }

                if (isMainFunction) {
                    createConfigurationFromMain(project, function.fqName?.asString()!!).checkConfiguration()

                    assertNotNull(
                        "$file: Kotlin configuration producer should produce configuration for ${function.fqName?.asString()}",
                        KotlinRunConfigurationProducer.getEntryPointContainer(function),
                    )
                } else {
                    try {
                        createConfigurationFromMain(project, function.fqName?.asString()!!).checkConfiguration()
                        fail(
                            "$file: configuration for function ${function.fqName?.asString()} at least shouldn't pass checkConfiguration()",
                        )
                    } catch (expected: Throwable) {
                    }

                    if (function.containingFile.text.startsWith("// entryPointExists")) {
                        assertNotNull(
                            "$file: Kotlin configuration producer should produce configuration for ${function.fqName?.asString()}",
                            KotlinRunConfigurationProducer.getEntryPointContainer(function),
                        )
                    } else {
                        assertNull(
                            "Kotlin configuration producer shouldn't produce configuration for ${function.fqName?.asString()}",
                            KotlinRunConfigurationProducer.getEntryPointContainer(function),
                        )
                    }
                }
            }
        }

        private fun createConfigurationFromMain(project: Project, mainFqn: String): KotlinRunConfiguration {
            val mainFunction = KotlinTopLevelFunctionFqnNameIndex.getInstance().get(mainFqn, project, project.allScope()).first()
            return createConfigurationFromElement(mainFunction) as KotlinRunConfiguration
        }
    }

    override fun getTestDataDirectory() = IDEA_TEST_DATA_DIR.resolve("run")
}
