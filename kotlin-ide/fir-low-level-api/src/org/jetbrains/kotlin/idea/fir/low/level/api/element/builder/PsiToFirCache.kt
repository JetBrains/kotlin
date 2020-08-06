/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.low.level.api.element.builder

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.fir.FirElement
import org.jetbrains.kotlin.fir.declarations.FirDeclaration
import org.jetbrains.kotlin.fir.declarations.FirFile
import org.jetbrains.kotlin.fir.psi
import org.jetbrains.kotlin.fir.realPsi
import org.jetbrains.kotlin.fir.references.*
import org.jetbrains.kotlin.fir.types.FirErrorTypeRef
import org.jetbrains.kotlin.fir.types.FirTypeRef
import org.jetbrains.kotlin.fir.types.FirUserTypeRef
import org.jetbrains.kotlin.fir.types.impl.FirResolvedTypeRefImpl
import org.jetbrains.kotlin.fir.visitors.FirVisitorVoid
import org.jetbrains.kotlin.idea.fir.low.level.api.file.builder.ModuleFileCache
import org.jetbrains.kotlin.idea.fir.low.level.api.util.isErrorElement
import java.util.concurrent.ConcurrentHashMap
import org.jetbrains.kotlin.idea.util.getElementTextInContext
import org.jetbrains.kotlin.psi.*

/**
 * Belongs to a [org.jetbrains.kotlin.idea.fir.low.level.api.FirModuleResolveState]
 */
internal class PsiToFirCache(private val moduleFileCache: ModuleFileCache) {
    private val caches = ConcurrentHashMap<KtFile, FileCache>()

    fun getCachedMapping(element: KtElement): FirElement? {
        val ktFile = element.containingKtFile
        val cache = caches[ktFile]
        return cache?.getCachedMapping(element)
    }

    fun getFir(element: KtElement, containerFir: FirDeclaration, firFile: FirFile): FirElement {
        val ktFile = element.containingKtFile
        val cache = caches.getOrPut(ktFile) { FileCache(ktFile, firFile, moduleFileCache) }
        return cache.getFir(element, containerFir)
    }

    //todo for completion only
    fun recordElementsForCompletionFrom(containerFir: FirDeclaration, firFile: FirFile, ktFile: KtFile) {
        val cache = caches.getOrPut(ktFile) { FileCache(ktFile, firFile, moduleFileCache) }
        cache.recordElementsForCompletionFrom(containerFir)
    }
}

internal class FileCache(val ktFile: KtFile, firFile: FirFile, moduleFileCache: ModuleFileCache) {
    private val cache: ConcurrentHashMap<KtElement, FirElement> = ConcurrentHashMap()

    fun getCachedMapping(ktElement: KtElement): FirElement? {
        require(ktElement.containingKtFile === ktFile)
        return cache[ktElement]
    }

    fun getFir(element: KtElement, containerFir: FirDeclaration): FirElement {
        val psi = when {
            element is KtPropertyDelegate -> element.expression ?: element
            element is KtQualifiedExpression && element.selectorExpression is KtCallExpression -> {
                /*
                 KtQualifiedExpression with KtCallExpression in selector transformed in FIR to FirFunctionCall expression
                 Which will have a receiver as qualifier
                 */
                element.selectorExpression ?: error("Incomplete code:\n${element.getElementTextInContext()}")
            }
            else -> element
        }
        cache[psi]?.let { return it }
        recordElementsFrom(containerFir)

        val (current, mappedFir) = psi.getFirOfClosestParent()
            ?: error("FirElement is not found for:\n${element.getElementTextInContext()}")
        if (current !== element) {
            cache(current, mappedFir)
        }

        return mappedFir
    }

    fun cache(psi: KtElement, fir: FirElement) {
        // todo make it thread safe
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

    fun recordElementsForCompletionFrom(firDeclaration: FirDeclaration) {
        recordElementsFrom(firDeclaration)
    }

    private fun recordElementsFrom(firDeclaration: FirDeclaration) {
        firDeclaration.accept(elementsRecorder)
    }

    private val elementsRecorder = object : FirVisitorVoid() {
        override fun visitElement(element: FirElement) {
            (element.realPsi as? KtElement)?.let { psi ->
                cache(psi, element)
            }
            element.acceptChildren(this)
        }

        override fun visitReference(reference: FirReference) {}
        override fun visitControlFlowGraphReference(controlFlowGraphReference: FirControlFlowGraphReference) {}
        override fun visitNamedReference(namedReference: FirNamedReference) {}
        override fun visitResolvedNamedReference(resolvedNamedReference: FirResolvedNamedReference) {}
        override fun visitDelegateFieldReference(delegateFieldReference: FirDelegateFieldReference) {}
        override fun visitBackingFieldReference(backingFieldReference: FirBackingFieldReference) {}
        override fun visitSuperReference(superReference: FirSuperReference) {}
        override fun visitThisReference(thisReference: FirThisReference) {}
        override fun visitErrorTypeRef(errorTypeRef: FirErrorTypeRef) {}

        override fun visitUserTypeRef(userTypeRef: FirUserTypeRef) {
            userTypeRef.acceptChildren(this)
        }
    }

    private fun KtElement.getFirOfClosestParent(): Pair<KtElement, FirElement>? {
        var current: PsiElement? = this
        while (current is KtElement) {
            val mappedFir = cache[current]
            if (mappedFir != null) {
                return current to mappedFir
            }
            current = current.parent
        }

        return null
    }
}

