/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForFacade
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.uast.*
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.visitor.UastVisitor

// TODO: can be commonized once annotation is handled
sealed class AbstractFirKotlinUClass(
    givenParent: UElement?
) : KotlinAbstractUElement(givenParent), UClass, UAnchorOwner {
    override val uAnnotations: List<UAnnotation>
        get() {
            // TODO: Not yet implemented
            return emptyList()
        }

    override val uastDeclarations: List<UDeclaration> by lz {
        mutableListOf<UDeclaration>().apply {
            addAll(fields)
            addAll(initializers)
            addAll(methods)
            addAll(innerClasses)
        }
    }

    abstract val ktClass: KtClassOrObject?

    override val uastSuperTypes: List<UTypeReferenceExpression>
        get() = ktClass?.superTypeListEntries.orEmpty().mapNotNull { it.typeReference }.map {
            KotlinUTypeReferenceExpression(it, this)
        }

    val delegateExpressions: List<UExpression>
        get() = ktClass?.superTypeListEntries.orEmpty()
            .filterIsInstance<KtDelegatedSuperTypeEntry>()
            .map { KotlinSupertypeDelegationUExpression(it, this) }

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitClass(this)) return
        delegateExpressions.acceptList(visitor)
        uAnnotations.acceptList(visitor)
        uastDeclarations.acceptList(visitor)
        visitor.afterVisitClass(this)
    }

    companion object {
        fun create(psi: KtLightClass, givenParent: UElement?): UClass {
            return when (psi) {
                // TODO: PsiAnonymousClass
                // TODO: Script
                else ->
                    FirKotlinUClass(psi, givenParent)
            }
        }
    }
}

// TODO: can be commonized once *KotlinUClass is commonized
class FirKotlinUClass(
    override val javaPsi: KtLightClass,
    givenParent: UElement?,
) : AbstractFirKotlinUClass(givenParent), PsiClass by javaPsi {
    override val ktClass: KtClassOrObject? = javaPsi.kotlinOrigin

    override val psi = unwrap<UClass, PsiClass>(javaPsi)

    override fun getSourceElement() = sourcePsi

    override fun getOriginalElement(): PsiElement? = sourcePsi?.originalElement

    override fun getNameIdentifier(): PsiIdentifier = UastLightIdentifier(psi, ktClass)

    override fun getContainingFile(): PsiFile = unwrapFakeFileForLightClass(psi.containingFile)

    override val uastAnchor: UIdentifier? by lz {
        getIdentifierSourcePsi()?.let {
            KotlinUIdentifier(nameIdentifier, it, this)
        }
    }

    private fun getIdentifierSourcePsi(): PsiElement? {
        ktClass?.nameIdentifier?.let { return it }
        (ktClass as? KtObjectDeclaration)?.getObjectKeyword()?.let { return it }
        return null
    }

    override fun getInnerClasses(): Array<UClass> {
        // filter DefaultImpls to avoid processing same methods from original interface multiple times
        // filter Enum entry classes to avoid duplication with PsiEnumConstant initializer class
        return psi.innerClasses.filter {
            it.name != JvmAbi.DEFAULT_IMPLS_CLASS_NAME && !it.isEnumEntryLightClass
        }.mapNotNull {
            languagePlugin?.convertOpt<UClass>(it, this)
        }.toTypedArray()
    }

    override fun getSuperClass(): UClass? {
        return super.getSuperClass()
    }

    override fun getFields(): Array<UField> {
        return super.getFields()
    }

    override fun getInitializers(): Array<UClassInitializer> {
        // TODO: why not just emptyList()? Kotlin class won't have <clinit>?
        return super.getInitializers()
    }

    override fun getMethods(): Array<UMethod> {
        val hasPrimaryConstructor = ktClass?.hasPrimaryConstructor() ?: false
        var secondaryConstructorsCount = 0

        fun createUMethod(psiMethod: PsiMethod): UMethod {
            return if (psiMethod is KtLightMethod && psiMethod.isConstructor) {
                if (!hasPrimaryConstructor && secondaryConstructorsCount++ == 0)
                    FirKotlinSecondaryConstructorWithInitializersUMethod(ktClass, psiMethod, this)
                else
                    FirKotlinConstructorUMethod(ktClass, psiMethod, this)
            } else {
                languagePlugin?.convertOpt(psiMethod, this) ?: reportConvertFailure(psiMethod)
            }
        }

        fun isDelegatedMethod(psiMethod: PsiMethod) = psiMethod is KtLightMethod && psiMethod.isDelegated

        val result = ArrayList<UMethod>(javaPsi.methods.size)
        val handledKtDeclarations = mutableSetOf<PsiElement>()

        for (lightMethod in javaPsi.methods) {
            if (isDelegatedMethod(lightMethod)) continue
            val uMethod = createUMethod(lightMethod)
            result.add(uMethod)

            // Ensure we pick the main Kotlin origin, not the auxiliary one
            val kotlinOrigin = (lightMethod as? KtLightMethod)?.kotlinOrigin ?: uMethod.sourcePsi
            handledKtDeclarations.addIfNotNull(kotlinOrigin)
        }

        val ktDeclarations: List<KtDeclaration> = run ktDeclarations@{
            ktClass?.let { return@ktDeclarations it.declarations }
            (javaPsi as? KtLightClassForFacade)?.let { facade ->
                return@ktDeclarations facade.files.flatMap { file -> file.declarations }
            }
            emptyList()
        }

        ktDeclarations.asSequence()
            .filterNot { handledKtDeclarations.contains(it) }
            .mapNotNullTo(result) {
                baseResolveProviderService.baseKotlinConverter.convertDeclaration(it, this, arrayOf(UElement::class.java)) as? UMethod
            }

        return result.toTypedArray()
    }
}

// TODO: FirKotlinUAnonymousClass or FirKotlinUAnonymousObject ?

// TODO: FirKotlinScriptUClass

// TODO: FirKotlinInvalidUClass
