/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir

import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import org.jetbrains.kotlin.fir.FirRenderer
import org.jetbrains.kotlin.fir.builder.RawFirBuilder
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.dependenciesWithoutSelf
import org.jetbrains.kotlin.fir.java.FirJavaModuleBasedSession
import org.jetbrains.kotlin.fir.java.FirLibrarySession
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.java.JavaSymbolProvider
import org.jetbrains.kotlin.fir.java.declarations.FirJavaClass
import org.jetbrains.kotlin.fir.java.declarations.FirJavaMethod
import org.jetbrains.kotlin.fir.java.scopes.JavaClassEnhancementScope
import org.jetbrains.kotlin.fir.resolve.FirProvider
import org.jetbrains.kotlin.fir.resolve.FirSymbolProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirCompositeSymbolProvider
import org.jetbrains.kotlin.fir.resolve.impl.FirProviderImpl
import org.jetbrains.kotlin.fir.resolve.transformers.FirTotalResolveTransformer
import org.jetbrains.kotlin.fir.scopes.ProcessorAction
import org.jetbrains.kotlin.fir.scopes.impl.FirCompositeScope
import org.jetbrains.kotlin.fir.service
import org.jetbrains.kotlin.fir.symbols.impl.FirFunctionSymbol
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.isLibraryClasses
import org.jetbrains.kotlin.idea.caches.project.productionSourceInfo
import org.jetbrains.kotlin.idea.multiplatform.setupMppProjectFromDirStructure
import org.jetbrains.kotlin.idea.stubs.AbstractMultiModuleTest
import org.jetbrains.kotlin.idea.test.ConfigLibraryUtil
import org.jetbrains.kotlin.idea.test.PluginTestCaseBase
import org.jetbrains.kotlin.idea.util.projectStructure.allModules
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.test.KotlinTestUtils
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File

abstract class AbstractFirMultiModuleResolveTest : AbstractMultiModuleTest() {
    override fun getTestDataPath(): String {
        return File(PluginTestCaseBase.getTestDataPathBase(), "/fir/multiModule").path + File.separator
    }

    fun doTest(dirPath: String) {
        setupMppProjectFromDirStructure(File(dirPath))
        val useFullJdk = "full" in dirPath
        val jdkKind = if (useFullJdk) TestJdkKind.FULL_JDK else TestJdkKind.MOCK_JDK
        for (module in project.allModules().drop(1)) {
            ConfigLibraryUtil.configureSdk(
                module,
                PluginTestCaseBase.addJdk(testRootDisposable) { PluginTestCaseBase.jdk(jdkKind) }
            )
        }
        doFirResolveTest(dirPath)
    }

    private fun createSession(module: Module, provider: FirProjectSessionProvider): FirJavaModuleBasedSession {
        val moduleInfo = module.productionSourceInfo()!!
        return FirJavaModuleBasedSession(moduleInfo, provider, moduleInfo.contentScope())
    }

    private fun createLibrarySession(moduleInfo: IdeaModuleInfo, provider: FirProjectSessionProvider): FirLibrarySession {
        val contentScope = moduleInfo.contentScope()
        return FirLibrarySession(moduleInfo, provider, contentScope)
    }

    private fun doFirResolveTest(dirPath: String) {
        val firFiles = mutableListOf<FirFile>()
        val sessions = mutableListOf<FirJavaModuleBasedSession>()
        val provider = FirProjectSessionProvider(project)
        for (module in project.allModules().drop(1)) {
            val session = createSession(module, provider)
            sessions += session

            val builder = RawFirBuilder(session, stubMode = false)
            val psiManager = PsiManager.getInstance(project)

            val ideaModuleInfo = session.moduleInfo.cast<IdeaModuleInfo>()

            ideaModuleInfo.dependenciesWithoutSelf().forEach {
                if (it is IdeaModuleInfo && it.isLibraryClasses()) {
                    createLibrarySession(it, provider)
                }
            }

            val contentScope = ideaModuleInfo.contentScope()

            val files = FileTypeIndex.getFiles(KotlinFileType.INSTANCE, contentScope)

            println("Got vfiles: ${files.size}")
            files.forEach {
                val file = psiManager.findFile(it) as? KtFile ?: return@forEach
                val firFile = builder.buildFirFile(file)
                (session.service<FirProvider>() as FirProviderImpl).recordFile(firFile)
                firFiles += firFile
            }
        }
        println("Raw fir up, files: ${firFiles.size}")

        fun expectedTxtPath(virtualFile: VirtualFile): String {
            val virtualPath = virtualFile.path
            var result: String? = null
            val root = File(dirPath)
            for (file in root.walkTopDown()) {
                if (!file.isDirectory && file.name in virtualPath) {
                    result = file.absolutePath.replace(".kt", ".txt")
                }
            }
            return result!!
        }

        val transformer = FirTotalResolveTransformer()
        transformer.processFiles(firFiles)
        for (file in firFiles) {
            val firFileDump = StringBuilder().also { file.accept(FirRenderer(it), null) }.toString()
            val expectedPath = expectedTxtPath((file.psi as PsiFile).virtualFile)
            KotlinTestUtils.assertEqualsToFile(File(expectedPath), firFileDump)
        }
        val processedJavaClasses = mutableSetOf<FirJavaClass>()
        val javaFirDump = StringBuilder().also { builder ->
            val renderer = FirRenderer(builder)
            for (session in sessions) {
                val symbolProvider = session.service<FirSymbolProvider>() as FirCompositeSymbolProvider
                val javaProvider = symbolProvider.providers.filterIsInstance<JavaSymbolProvider>().first()
                for (javaClass in javaProvider.getJavaTopLevelClasses().sortedBy { it.name }) {
                    if (javaClass !is FirJavaClass || javaClass in processedJavaClasses) continue
                    val enhancementScope = javaClass.buildClassSpecificUseSiteScope(session).let {
                        when (it) {
                            is FirCompositeScope -> it.scopes.filterIsInstance<JavaClassEnhancementScope>().first()
                            is JavaClassEnhancementScope -> it
                            else -> null
                        }
                    }
                    if (enhancementScope == null) {
                        javaClass.accept(renderer, null)
                    } else {
                        renderer.visitMemberDeclaration(javaClass)
                        renderer.renderSupertypes(javaClass)
                        renderer.renderInBraces {
                            val renderedDeclarations = mutableListOf<FirDeclaration>()
                            for (declaration in javaClass.declarations) {
                                if (declaration in renderedDeclarations) continue
                                if (declaration !is FirJavaMethod) {
                                    declaration.accept(renderer, null)
                                    renderer.newLine()
                                    renderedDeclarations += declaration
                                } else {
                                    enhancementScope.processFunctionsByName(declaration.name) { symbol ->
                                        val enhanced = (symbol as? FirFunctionSymbol)?.fir
                                        if (enhanced != null && enhanced !in renderedDeclarations) {
                                            enhanced.accept(renderer, null)
                                            renderer.newLine()
                                            renderedDeclarations += enhanced
                                        }
                                        ProcessorAction.NEXT
                                    }
                                }
                            }
                        }
                    }
                    processedJavaClasses += javaClass
                }
            }
        }.toString()
        if (javaFirDump.isNotEmpty()) {
            KotlinTestUtils.assertEqualsToFile(File("$dirPath/extraDump.java.txt"), javaFirDump)
        }
    }
}
