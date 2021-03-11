/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.references

class KotlinFirReferenceContributor : KotlinReferenceProviderContributor {
    override fun registerReferenceProviders(registrar: KotlinPsiReferenceRegistrar) {
        with(registrar) {
            registerProvider(factory = ::KtFirSimpleNameReference)
            registerProvider(factory = ::KtFirForLoopInReference)
            registerProvider(factory = ::KtFirInvokeFunctionReference)
            registerProvider(factory = ::KtFirPropertyDelegationMethodsReference)
            registerProvider(factory = ::KtFirDestructuringDeclarationReference)
            registerProvider(factory = ::KtFirArrayAccessReference)
            registerProvider(factory = ::KtFirConstructorDelegationReference)
            registerProvider(factory = ::KtFirCollectionLiteralReference)
        }
    }
}
