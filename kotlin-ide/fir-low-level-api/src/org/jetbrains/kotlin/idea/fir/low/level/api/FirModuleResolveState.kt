/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api

import com.google.common.collect.MapMaker
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement
import org.jetbrains.kotlin.cfg.pseudocode.containingDeclarationForPseudocode
import org.jetbrains.kotlin.diagnostics.Diagnostic
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirDiagnostic
import org.jetbrains.kotlin.fir.analysis.diagnostics.FirPsiDiagnostic
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.extensions.BunchOfRegisteredExtensions
import org.jetbrains.kotlin.fir.extensions.extensionService
import org.jetbrains.kotlin.fir.extensions.registerExtensions
import org.jetbrains.kotlin.fir.java.FirProjectSessionProvider
import org.jetbrains.kotlin.fir.render
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.idea.caches.project.IdeaModuleInfo
import org.jetbrains.kotlin.idea.caches.project.LibrarySourceInfo
import org.jetbrains.kotlin.idea.caches.project.ModuleSourceInfo
import org.jetbrains.kotlin.idea.caches.project.getModuleInfo
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtTypeReference

abstract class FirModuleResolveState {
    internal abstract val sessionProvider: FirProjectSessionProvider

    internal fun getSession(psi: KtElement): FirSession {
        val moduleInfo = psi.getModuleInfo()
        return getSession(psi.project, moduleInfo)
    }

    internal fun getSession(project: Project, moduleInfo: IdeaModuleInfo): FirSession {
        sessionProvider.getSession(moduleInfo)?.let { return it }
        val lock = when (moduleInfo) {
            is ModuleSourceInfo -> moduleInfo.module
            is LibrarySourceInfo -> moduleInfo.library
            else -> TODO(moduleInfo.toString())
        }
        return synchronized(lock) {
            val session = sessionProvider.getSession(moduleInfo) ?: FirIdeJavaModuleBasedSession.create(
                project, moduleInfo, sessionProvider, moduleInfo.contentScope()
            ).also { moduleBasedSession ->
                sessionProvider.sessionCache[moduleInfo] = moduleBasedSession
            }
            session.also {
                it.extensionService.registerExtensions(BunchOfRegisteredExtensions.empty())
            }
        }
    }

    internal abstract operator fun get(psi: KtElement): FirElement?

    internal abstract fun getDiagnostics(psi: KtElement): List<Diagnostic>

    internal abstract fun hasDiagnosticsForFile(file: KtFile): Boolean

    internal abstract fun record(psi: KtElement, fir: FirElement)

    internal abstract fun record(psi: KtElement, diagnostic: Diagnostic)

    internal abstract fun setDiagnosticsForFile(file: KtFile, fir: FirFile, diagnostics: Iterable<FirDiagnostic<*>> = emptyList())
}

internal class FirModuleResolveStateImpl(override val sessionProvider: FirProjectSessionProvider) : FirModuleResolveState() {
    private val cache = mutableMapOf<KtElement, FirElement>()

    private val diagnosticCache = mutableMapOf<KtElement, MutableList<Diagnostic>>()

    private val diagnosedFiles = mutableMapOf<KtFile, Long>()

    override fun get(psi: KtElement): FirElement? = cache[psi]

    override fun getDiagnostics(psi: KtElement): List<Diagnostic> {
        return diagnosticCache[psi] ?: emptyList()
    }

    override fun hasDiagnosticsForFile(file: KtFile): Boolean {
        val previousStamp = diagnosedFiles[file] ?: return false
        if (file.modificationStamp == previousStamp) {
            return true
        }
        diagnosedFiles.remove(file)
        file.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                cache.remove(element)
                diagnosticCache.remove(element)
                element.acceptChildren(this)
                super.visitElement(element)
            }
        })
        return false
    }

    override fun record(psi: KtElement, fir: FirElement) {
        val existingFir = cache[psi]
        if (existingFir != null && existingFir !== fir) {
            when {
                existingFir is FirTypeRef && fir is FirTypeRef && psi is KtTypeReference -> {
                    // FirTypeRefs are often created during resolve
                    // a lot of them with have the same source
                    // we want to take the most "resolved one" here
                    if (fir is FirResolvedTypeRefImpl && existingFir !is FirResolvedTypeRefImpl) {
                        cache[psi] = fir
                    }
                }
                existingFir.isErrorElement && !fir.isErrorElement -> {
                    // TODO better handle error elements
                    // but for now just take first non-error one if such exist
                    cache[psi] = fir
                }
                existingFir.isErrorElement || fir.isErrorElement -> {
                    // do nothing and maybe upgrade to a non-error element in the branch above in the future
                }
                else -> {

                    if (DuplicatedFirSourceElementsException.IS_ENABLED) {
                        throw DuplicatedFirSourceElementsException(existingFir, fir, psi)
                    }
                }
            }
        }
        if (existingFir == null) {
            cache[psi] = fir
        }
    }

    override fun record(psi: KtElement, diagnostic: Diagnostic) {
        val list = diagnosticCache.getOrPut(psi) { mutableListOf() }
        list += diagnostic
    }

    override fun setDiagnosticsForFile(file: KtFile, fir: FirFile, diagnostics: Iterable<FirDiagnostic<*>>) {
        for (diagnostic in diagnostics) {
            require(diagnostic is FirPsiDiagnostic<*>)
            val psi = diagnostic.element.psi as? KtElement ?: continue
            record(psi, diagnostic.asPsiBasedDiagnostic())
        }

        diagnosedFiles[file] = file.modificationStamp
    }
}

class DuplicatedFirSourceElementsException(
    existingFir: FirElement,
    newFir: FirElement,
    psi: KtElement
) : IllegalStateException() {
    override val message: String? = """|The PSI element should be used only once as a real PSI source of FirElement,
       |the elements ${if (existingFir.source === newFir.source) "HAVE" else "DON'T HAVE"} the same instances of source elements 
       |
       |existing FIR element is $existingFir with text:
       |${existingFir.render().trim()}
       |
       |new FIR element is $newFir with text:
       | ${newFir.render().trim()}
       |
       |PSI element is $psi with text in context:
       |${psi.getElementTextInContext()}""".trimMargin()


    companion object {
        // The are some cases which are still generates FIR elements with duplicated source elements
        // Then such case is met, it's better to be fixed
        // but exception reporting can be easily disabled by setting this to false
        var IS_ENABLED = true
    }
}

internal fun KtElement.firResolveState(): FirModuleResolveState =
    FirIdeResolveStateService.getInstance(project).getResolveState(getModuleInfo())

