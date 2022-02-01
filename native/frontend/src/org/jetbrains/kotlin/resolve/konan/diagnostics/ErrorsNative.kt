/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.konan.diagnostics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.types.KotlinType

object ErrorsNative {
    @JvmField
    val THROWS_LIST_EMPTY = DiagnosticFactory0.create<KtElement>(Severity.ERROR)

    @JvmField
    val INCOMPATIBLE_THROWS_OVERRIDE = DiagnosticFactory1.create<KtElement, DeclarationDescriptor>(Severity.ERROR)

    @JvmField
    val INCOMPATIBLE_THROWS_INHERITED = DiagnosticFactory1.create<KtDeclaration, Collection<DeclarationDescriptor>>(Severity.ERROR)

    @JvmField
    val MISSING_EXCEPTION_IN_THROWS_ON_SUSPEND = DiagnosticFactory1.create<KtElement, FqName>(Severity.ERROR)

    @JvmField
    val INAPPLICABLE_SHARED_IMMUTABLE_PROPERTY = DiagnosticFactory0.create<KtElement>(Severity.ERROR)

    @JvmField
    val INAPPLICABLE_SHARED_IMMUTABLE_TOP_LEVEL = DiagnosticFactory0.create<KtElement>(Severity.ERROR)

    @JvmField
    val VARIABLE_IN_SINGLETON_WITHOUT_THREAD_LOCAL = DiagnosticFactory0.create<KtElement>(Severity.WARNING)

    @JvmField
    val INAPPLICABLE_THREAD_LOCAL = DiagnosticFactory0.create<KtElement>(Severity.ERROR)

    @JvmField
    val INAPPLICABLE_THREAD_LOCAL_TOP_LEVEL = DiagnosticFactory0.create<KtElement>(Severity.ERROR)

    @JvmField
    val VARIABLE_IN_ENUM = DiagnosticFactory0.create<KtElement>(Severity.WARNING)

    @JvmField
    val INVALID_CHARACTERS_NATIVE = DiagnosticFactoryForDeprecation1.create<PsiElement, String>(LanguageFeature.ProhibitInvalidCharsInNativeIdentifiers)


    @JvmField
    val LEAKING_PHANTOM_TYPE = DiagnosticFactory1.create<KtCallableDeclaration, KotlinType>(
        Severity.ERROR,
        PositioningStrategies.DECLARATION_RETURN_TYPE,
    )

    @JvmField
    val LEAKING_PHANTOM_TYPE_IN_TYPE_PARAMETERS = DiagnosticFactory1.create<KtTypeParameter, KotlinType>(Severity.ERROR)

    @JvmField
    val LEAKING_PHANTOM_TYPE_IN_SUPERTYPES = DiagnosticFactory1.create<KtClassOrObject, KotlinType>(
        Severity.ERROR,
        PositioningStrategies.SUPERTYPES_LIST,
    )

    @JvmField
    val PHANTOM_CLASSIFIER = DiagnosticFactory1.create<PsiElement, KotlinType>(Severity.ERROR)

    init {
        Errors.Initializer.initializeFactoryNames(ErrorsNative::class.java)
    }
}