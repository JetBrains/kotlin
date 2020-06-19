/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

import com.intellij.openapi.components.service
import com.intellij.openapi.project.IndexNotReadyException
import com.intellij.psi.ContributedReferenceHost
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceService
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.ConcurrentFactoryMap
import com.intellij.util.containers.ContainerUtil
import com.intellij.util.containers.MultiMap
import org.jetbrains.kotlin.psi.KotlinReferenceProvidersService
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.utils.SmartList

interface KotlinPsiReferenceProvider {
    fun getReferencesByElement(element: PsiElement): Array<PsiReference>
}

interface KotlinReferenceProviderContributor {
    fun registerReferenceProviders(registrar: KotlinPsiReferenceRegistrar)

    companion object {
        fun getInstance(): KotlinReferenceProviderContributor = service()
    }
}


class KotlinPsiReferenceRegistrar {
    val providers: MultiMap<Class<out PsiElement>, KotlinPsiReferenceProvider> = MultiMap.create()

    inline fun <reified E : KtElement> registerProvider(crossinline factory: (E) -> PsiReference?) {
        registerMultiProvider<E> { element ->
            factory(element)?.let { reference -> arrayOf(reference) } ?: PsiReference.EMPTY_ARRAY
        }
    }

    inline fun <reified E : KtElement>  registerMultiProvider(crossinline factory: (E) -> Array<PsiReference>) {
        val provider: KotlinPsiReferenceProvider = object : KotlinPsiReferenceProvider {
            override fun getReferencesByElement(element: PsiElement): Array<PsiReference> {
                return factory(element as E)
            }
        }

        registerMultiProvider(E::class.java, provider)
    }

    fun registerMultiProvider(klass: Class<out PsiElement>, provider: KotlinPsiReferenceProvider) {
        providers.putValue(klass, provider)
    }
}

class KtIdeReferenceProviderService : KotlinReferenceProvidersService() {
    private val originalProvidersBinding: MultiMap<Class<out PsiElement>, KotlinPsiReferenceProvider>
    private val providersBindingCache: Map<Class<out PsiElement>, List<KotlinPsiReferenceProvider>>

    init {
        val registrar = KotlinPsiReferenceRegistrar()
        KotlinReferenceProviderContributor.getInstance().registerReferenceProviders(registrar)
        originalProvidersBinding = registrar.providers

        providersBindingCache = ConcurrentFactoryMap.createMap<Class<out PsiElement>, List<KotlinPsiReferenceProvider>> { klass ->
            val result = SmartList<KotlinPsiReferenceProvider>()
            for (bindingClass in originalProvidersBinding.keySet()) {
                if (bindingClass.isAssignableFrom(klass)) {
                    result.addAll(originalProvidersBinding.get(bindingClass))
                }
            }
            result
        }
    }

    private fun doGetKotlinReferencesFromProviders(context: PsiElement): Array<PsiReference> {
        val providers: List<KotlinPsiReferenceProvider>? = providersBindingCache[context.javaClass]
        if (providers.isNullOrEmpty()) return PsiReference.EMPTY_ARRAY

        val result = SmartList<PsiReference>()
        for (provider in providers) {
            try {
                result.addAll(provider.getReferencesByElement(context))
            } catch (ignored: IndexNotReadyException) {
                // Ignore and continue to next provider
            }
        }

        if (result.isEmpty()) {
            return PsiReference.EMPTY_ARRAY
        }

        return result.toTypedArray()
    }

    override fun getReferences(psiElement: PsiElement): Array<PsiReference> {
        if (psiElement is ContributedReferenceHost) {
            return ReferenceProvidersRegistry.getReferencesFromProviders(psiElement, PsiReferenceService.Hints.NO_HINTS)
        }

        return CachedValuesManager.getCachedValue(psiElement) {
            CachedValueProvider.Result.create(
                doGetKotlinReferencesFromProviders(psiElement),
                PsiModificationTracker.MODIFICATION_COUNT
            )
        }
    }
}