/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.ModuleTestCase
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.idea.caches.project.LibraryInfo
import org.jetbrains.kotlin.idea.caches.project.LibrarySourceInfo
import org.jetbrains.kotlin.idea.caches.project.getNullableModuleInfo
import org.jetbrains.kotlin.idea.decompiler.navigation.NavigationChecker.Companion.checkAnnotatedCode
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.projectStructure.getModuleDir
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import org.jetbrains.kotlin.test.util.addDependency
import org.jetbrains.kotlin.test.util.jarRoot
import org.jetbrains.kotlin.test.util.projectLibrary
import org.junit.Assert
import java.io.File

class NavigationWithMultipleCustomLibrariesTest : AbstractNavigationToSourceOrDecompiledTest() {

    override val testDataPath = PluginTestCaseBase.getTestDataPathBase() + "/decompiler/navigationMultipleLibs/"

    fun testNavigationToDecompiled() {
        doTest(false, "expected.decompiled")
    }

    fun testNavigationToLibrarySources() {
        doTest(true, "expected.sources")
    }

    override fun createProjectLib(libraryName: String, withSources: Boolean): Library {
        val librarySrc = testDataPath + "libSrc"

        val libraryJar = MockLibraryUtil.compileJvmLibraryToJar(librarySrc, libraryName, addSources = withSources)
        val jarRoot = libraryJar.jarRoot
        return projectLibrary(libraryName, jarRoot, jarRoot.findChild("src").takeIf { withSources })
    }
}

class NavigationWithMultipleRuntimesTest : AbstractNavigationToSourceOrDecompiledTest() {

    override val testDataPath = PluginTestCaseBase.getTestDataPathBase() + "/decompiler/navigationMultipleRuntimes/"

    fun testNavigationToDecompiled() {
        doTest(false, "expected.decompiled")
    }

    fun testNavigationToLibrarySources() {
        doTest(true, "expected.sources")
    }

    override fun createProjectLib(libraryName: String, withSources: Boolean): Library {
        val libraryJar = ForTestCompileRuntime.runtimeJarForTests().copyTo(File(createTempDirectory(), "$libraryName.jar"))
        val jarUrl = libraryJar.jarRoot
        return runWriteAction {
            val library = ProjectLibraryTable.getInstance(project).createLibrary(libraryName)
            val modifiableModel = library.modifiableModel
            modifiableModel.addRoot(jarUrl, OrderRootType.CLASSES)
            if (withSources) {
                val sourcesJar = ForTestCompileRuntime.runtimeSourcesJarForTests().copyTo(File(createTempDirectory(), "$libraryName-sources.jar"))
                modifiableModel.addRoot(sourcesJar.jarRoot, OrderRootType.SOURCES)
            }
            modifiableModel.commit()
            library
        }
    }
}

abstract class AbstractNavigationToSourceOrDecompiledTest: AbstractNavigationWithMultipleLibrariesTest() {
    fun doTest(withSources: Boolean, expectedFileName: String) {
        val srcPath = testDataPath + "src"
        val moduleA = module("moduleA", srcPath)
        val moduleB = module("moduleB", srcPath)

        moduleA.addDependency(createProjectLib("libA", withSources))
        moduleB.addDependency(createProjectLib("libB", withSources))

        // navigation code works by providing first matching declaration from indices
        // that's we need to check references in both modules to guard against possibility of code breaking
        // while tests pass by chance
        checkReferencesInModule(moduleA, "libA", expectedFileName)
        checkReferencesInModule(moduleB, "libB", expectedFileName)
    }

    abstract fun createProjectLib(libraryName: String, withSources: Boolean): Library
}

class NavigationToSingleJarInMultipleLibrariesTest : AbstractNavigationWithMultipleLibrariesTest() {

    override val testDataPath = "${PluginTestCaseBase.getTestDataPathBase()}/multiModuleReferenceResolve/sameJarInDifferentLibraries/"

    fun testNavigatingToLibrarySharingSameJarOnlyOneHasSourcesAttached() {
        val srcPath = testDataPath + "src"
        val moduleA = module("m1", srcPath)
        val moduleB = module("m2", srcPath)
        val moduleC = module("m3", srcPath)

        val sharedJar = MockLibraryUtil.compileJvmLibraryToJar(testDataPath + "libSrc", "sharedJar", addSources = true)
        val jarRoot = sharedJar.jarRoot

        moduleA.addDependency(projectLibrary("libA", jarRoot))
        moduleB.addDependency(projectLibrary("libB", jarRoot, jarRoot.findChild("src")!!))
        moduleC.addDependency(projectLibrary("libC", jarRoot))

        val expectedFile = File(testDataPath + "expected.sources")
        checkAnnotatedCode(findSourceFile(moduleA), expectedFile)
        checkAnnotatedCode(findSourceFile(moduleB), expectedFile)
        checkAnnotatedCode(findSourceFile(moduleC), expectedFile)
    }
}

abstract class AbstractNavigationWithMultipleLibrariesTest : ModuleTestCase() {
    abstract val testDataPath: String

    protected fun module(name: String, srcPath: String) = createModuleFromTestData(srcPath, name, StdModuleTypes.JAVA, true)!!

    protected fun checkReferencesInModule(module: Module, libraryName: String, expectedFileName: String) {
        checkAnnotatedCode(findSourceFile(module), File(testDataPath + expectedFileName)) {
            checkLibraryName(it, libraryName)
        }
    }
}

private fun checkLibraryName(referenceTarget: PsiElement, expectedName: String) {
    val navigationFile = referenceTarget.navigationElement.containingFile ?: return
    val libraryInfo = navigationFile.getNullableModuleInfo()
    val libraryName = when (libraryInfo) {
        is LibraryInfo -> libraryInfo.library.name
        is LibrarySourceInfo -> libraryInfo.library.name
        else -> error("Couldn't get library name")
    }
    Assert.assertEquals("Referenced code from unrelated library: ${referenceTarget.text}", expectedName, libraryName)
}

private fun findSourceFile(module: Module): PsiFile {
    val ioFile = File(module.getModuleDir()).listFiles().first()
    val vFile = LocalFileSystem.getInstance().findFileByIoFile(ioFile)!!
    return PsiManager.getInstance(module.project).findFile(vFile)!!
}
