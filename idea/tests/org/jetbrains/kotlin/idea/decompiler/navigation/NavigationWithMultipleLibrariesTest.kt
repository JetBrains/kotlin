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

package org.jetbrains.kotlin.idea.decompiler.navigation

import com.intellij.openapi.module.Module
import com.intellij.openapi.module.StdModuleTypes
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.testFramework.ModuleTestCase
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.idea.caches.resolve.LibraryInfo
import org.jetbrains.kotlin.idea.caches.resolve.LibrarySourceInfo
import org.jetbrains.kotlin.idea.caches.resolve.getNullableModuleInfo
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.projectStructure.getModuleDir
import org.jetbrains.kotlin.test.MockLibraryUtil
import org.jetbrains.kotlin.test.testFramework.runWriteAction
import org.junit.Assert
import java.io.File

class NavigationWithMultipleCustomLibrariesTest : AbstractNavigationWithMultipleLibrariesTest() {

    override val testDataPath = PluginTestCaseBase.getTestDataPathBase() + "/decompiler/navigationMultipleLibs/"

    fun testNavigationToDecompiled() {
        doTest(false, "expected.decompiled")
    }

    fun testNavigationToLibrarySources() {
        doTest(true, "expected.sources")
    }

    override fun createProjectLib(libraryName: String, withSources: Boolean): Library {
        val librarySrc = testDataPath + "libSrc"

        val libraryJar = MockLibraryUtil.compileLibraryToJar(librarySrc, libraryName, withSources, false, false)
        val jarRoot = libraryJar.jarRoot()
        return runWriteAction {
            val library = ProjectLibraryTable.getInstance(project).createLibrary(libraryName)
            val modifiableModel = library.modifiableModel
            modifiableModel.addRoot(jarRoot, OrderRootType.CLASSES)
            if (withSources) {
                modifiableModel.addRoot(jarRoot.findChild("src")!!, OrderRootType.SOURCES)
            }
            modifiableModel.commit()
            library
        }
    }
}

class NavigationWithMultipleRuntimesTest : AbstractNavigationWithMultipleLibrariesTest() {

    override val testDataPath = PluginTestCaseBase.getTestDataPathBase() + "/decompiler/navigationMultipleRuntimes/"

    fun testNavigationToDecompiled() {
        doTest(false, "expected.decompiled")
    }

    fun testNavigationToLibrarySources() {
        doTest(true, "expected.sources")
    }

    override fun createProjectLib(libraryName: String, withSources: Boolean): Library {
        val libraryJar = ForTestCompileRuntime.runtimeJarForTests().copyTo(File(createTempDirectory(), "$libraryName.jar"))
        val jarUrl = libraryJar.jarRoot()
        return runWriteAction {
            val library = ProjectLibraryTable.getInstance(project).createLibrary(libraryName)
            val modifiableModel = library.modifiableModel
            modifiableModel.addRoot(jarUrl, OrderRootType.CLASSES)
            if (withSources) {
                val sourcesJar = ForTestCompileRuntime.runtimeSourcesJarForTests().copyTo(File(createTempDirectory(), "$libraryName-sources.jar"))
                modifiableModel.addRoot(sourcesJar.jarRoot(), OrderRootType.SOURCES)
            }
            modifiableModel.commit()
            library
        }
    }
}

abstract class AbstractNavigationWithMultipleLibrariesTest : ModuleTestCase() {

    abstract val testDataPath: String

    fun doTest(withSources: Boolean, expectedFileName: String) {
        val srcPath = testDataPath + "src"
        val moduleA = module("moduleA", srcPath)
        val moduleB = module("moduleB", srcPath)

        addDependencyOnProjectLibrary(moduleA, "libA", withSources)
        addDependencyOnProjectLibrary(moduleB, "libB", withSources)

        // navigation code works by providing first matching declaration from indices
        // that's we need to check references in both modules to guard against possibility of code breaking
        // while tests pass by chance
        checkReferencesInModule(moduleA, "libA", expectedFileName)
        checkReferencesInModule(moduleB, "libB", expectedFileName)
    }

    private fun module(name: String, srcPath: String) = createModuleFromTestData(srcPath, name, StdModuleTypes.JAVA, true)!!

    private fun checkReferencesInModule(moduleB: Module, libraryName: String, expectedFileName: String) {
        NavigationChecker.checkAnnotatedCode(findSourceFile(moduleB), File(testDataPath + expectedFileName)) {
            checkLibraryName(it, libraryName)
        }
    }

    private fun findSourceFile(moduleA: Module): PsiFile {
        val ioFile = File(moduleA.getModuleDir()).listFiles().first()
        val vFile = LocalFileSystem.getInstance().findFileByIoFile(ioFile)!!
        return PsiManager.getInstance(project).findFile(vFile)!!
    }

    private fun addDependencyOnProjectLibrary(mainModule: Module, libraryName: String, withSources: Boolean) {
        val library = createProjectLib(libraryName, withSources)
        ModuleRootModificationUtil.addDependency(mainModule, library, DependencyScope.COMPILE, false)
    }

    abstract fun createProjectLib(libraryName: String, withSources: Boolean): Library
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

private fun File.jarRoot() = StandardFileSystems.getJarRootForLocalFile(LocalFileSystem.getInstance().findFileByIoFile(this)!!)!!
