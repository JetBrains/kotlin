// Copyright 2000-2017 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.kotlin.idea.debugger.sequence

import com.intellij.debugger.streams.test.StreamChainBuilderTestCase
import com.intellij.debugger.streams.wrapper.StreamChain
import com.intellij.debugger.streams.wrapper.StreamChainBuilder
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.projectRoots.Sdk
import com.intellij.openapi.roots.impl.libraries.ProjectLibraryTable
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.impl.PsiManagerEx
import com.intellij.psi.impl.file.impl.FileManagerImpl
import com.intellij.psi.impl.source.PsiFileImpl
import com.intellij.testFramework.LightPlatformTestCase
import com.intellij.testFramework.PsiTestUtil
import junit.framework.TestCase
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.idea.caches.project.LibraryModificationTracker
import org.jetbrains.kotlin.idea.decompiler.KotlinDecompiledFileViewProvider
import org.jetbrains.kotlin.idea.decompiler.KtDecompiledFile
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import java.io.File
import java.nio.file.Paths
import java.util.*

abstract class KotlinPsiChainBuilderTestCase(private val relativePath: String) : StreamChainBuilderTestCase() {
    override fun getTestDataPath(): String =
        Paths.get(File("").absolutePath, "idea/testData/debugger/sequence/psi/$relativeTestPath/").toString()

    override fun getFileExtension(): String = ".kt"
    abstract val kotlinChainBuilder: StreamChainBuilder
    override fun getChainBuilder(): StreamChainBuilder = kotlinChainBuilder
    private val stdLibName = "kotlin-stdlib"

    protected abstract fun doTest()

    override fun tearDown() {
        doKotlinTearDown(LightPlatformTestCase.getProject(), { super.tearDown() })
    }

    final override fun getRelativeTestPath(): String = relativePath

    override fun setUp() {
        super.setUp()
        ApplicationManager.getApplication().runWriteAction {
            if (ProjectLibraryTable.getInstance(LightPlatformTestCase.getProject()).getLibraryByName(stdLibName) == null) {
                val stdLibPath = ForTestCompileRuntime.runtimeJarForTests()
                PsiTestUtil.addLibrary(
                    testRootDisposable,
                    LightPlatformTestCase.getModule(),
                    stdLibName,
                    stdLibPath.parent,
                    stdLibPath.name
                )
            }
        }
        LibraryModificationTracker.getInstance(LightPlatformTestCase.getProject()).incModificationCount()
    }


    override fun getProjectJDK(): Sdk {
        return PluginTestCaseBase.mockJdk9()
    }

    private fun doKotlinTearDown(project: Project, runnable: () -> Unit) {
        unInvalidateBuiltinsAndStdLib(project) {
            runnable()
        }
    }

    private fun unInvalidateBuiltinsAndStdLib(project: Project, runnable: () -> Unit) {
        val stdLibViewProviders = HashSet<KotlinDecompiledFileViewProvider>()
        val vFileToViewProviderMap =
            ((PsiManager.getInstance(project) as PsiManagerEx).fileManager as FileManagerImpl).vFileToViewProviderMap
        for ((file, viewProvider) in vFileToViewProviderMap) {
            if (file.isStdLibFile && viewProvider is KotlinDecompiledFileViewProvider) {
                stdLibViewProviders.add(viewProvider)
            }
        }

        runnable()

        // Base tearDown() invalidates builtins and std-lib files. Restore them with brute force.
        fun unInvalidateFile(file: PsiFileImpl) {
            val field = PsiFileImpl::class.java.getDeclaredField("myInvalidated")!!
            field.isAccessible = true
            field.set(file, false)
        }

        stdLibViewProviders.forEach {
            it.allFiles.forEach { unInvalidateFile(it as KtDecompiledFile) }
            vFileToViewProviderMap[it.virtualFile] = it
        }
    }

    private val VirtualFile.isStdLibFile: Boolean get() = presentableUrl.contains("kotlin-runtime.jar")

    abstract class Positive(relativePath: String) : KotlinPsiChainBuilderTestCase(relativePath) {
        override fun doTest() {
            val chains = buildChains()
            checkChains(chains)
        }

        private fun checkChains(chains: MutableList<StreamChain>) {
            TestCase.assertFalse(chains.isEmpty())
        }
    }

    abstract class Negative(relativePath: String) : KotlinPsiChainBuilderTestCase(relativePath) {
        override fun doTest() {
            val elementAtCaret = configureAndGetElementAtCaret()
            TestCase.assertFalse(chainBuilder.isChainExists(elementAtCaret))
            TestCase.assertTrue(chainBuilder.build(elementAtCaret).isEmpty())
        }
    }
}
