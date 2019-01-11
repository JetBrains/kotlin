/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.ModuleTestCase
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.dependenciesWithoutSelf
import org.jetbrains.kotlin.fir.doFirResolveTestBench
import org.jetbrains.kotlin.fir.java.FirJavaModuleBasedSession
import org.jetbrains.kotlin.fir.java.FirLibrarySession
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.isLibraryClasses
import org.jetbrains.kotlin.idea.caches.project.productionSourceInfo
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

class FirTotalKotlinResolveInIdeTest : ModuleTestCase() {
    private val forbiddenDirectories = listOf("testdata", "resources")

    private val projectRootFile = File(".")

    override fun setUpModule() {
        super.setUpModule()

        ModuleRootModificationUtil.addContentRoot(module, VfsUtil.findFileByIoFile(projectRootFile, true)!!)

        ModuleRootModificationUtil.updateModel(module) {
            projectRootFile.walkTopDown().onEnter {
                it.name.toLowerCase() !in forbiddenDirectories
            }.filter {
                it.isDirectory
            }.forEach { dir ->
                val vdir by lazy { VfsUtil.findFileByIoFile(dir, true)!! }
                if (dir.name in setOf("src", "test", "tests")) {
                    it.contentEntries.single().addSourceFolder(vdir, false)
                } else if (dir.name in setOf("build")) {
                    it.contentEntries.single().addExcludeFolder(vdir)
                }
            }
        }

    }

    private fun createSession(): FirSession {
        val moduleInfo = module.productionSourceInfo()!!
        val sessionProvider = FirProjectSessionProvider(project)
        moduleInfo.dependenciesWithoutSelf().forEach {
            if (it is IdeaModuleInfo && it.isLibraryClasses()) {
                FirLibrarySession(it, sessionProvider, it.contentScope())
            }
        }

        return FirJavaModuleBasedSession(moduleInfo, sessionProvider, moduleInfo.contentScope())
    }

    fun testTotalKotlin() {
        val session = createSession()

        val firFiles = mutableListOf<FirFile>()
        val builder = RawFirBuilder(session, stubMode = true)
        val psiManager = PsiManager.getInstance(project)

        val files = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, GlobalSearchScope.moduleScope(module))


        println("Got vfiles: ${files.size}")
        files.forEach {
            val file = psiManager.findFile(it) as? KtFile ?: return@forEach
            try {
                val firFile = builder.buildFirFile(file)
                (session.service<FirProvider>() as FirProviderImpl).recordFile(firFile)
                firFiles += firFile
            } catch (e: Exception) {
                System.err.println("Error building fir for $it")
                e.printStackTrace()
            }
        }

        println("Raw fir up, files: ${firFiles.size}")

        doFirResolveTestBench(firFiles, FirTotalResolveTransformer().transformers)
    }
}