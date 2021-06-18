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
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.utils.addIfNotNull
import org.jetbrains.uast.*

abstract class BaseKotlinUClass(
    psi: KtLightClass,
    givenParent: UElement?
) : AbstractKotlinUClass(givenParent), PsiClass by psi {
    final override val ktClass = psi.kotlinOrigin

    override val javaPsi: KtLightClass = psi

    override val sourcePsi: KtClassOrObject? = ktClass

    override val psi = unwrap<UClass, PsiClass>(psi)

    override fun getSourceElement() = sourcePsi ?: this

    override fun getOriginalElement(): PsiElement? = super.getOriginalElement()

    override fun getNameIdentifier(): PsiIdentifier? = UastLightIdentifier(psi, ktClass)

    override fun getContainingFile(): PsiFile? = unwrapFakeFileForLightClass(psi.containingFile)

    override val uastAnchor by lazy { getIdentifierSourcePsi()?.let { KotlinUIdentifier(nameIdentifier, it, this) } }

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

    override fun getSuperClass(): UClass? = super.getSuperClass()
    override fun getFields(): Array<UField> = super.getFields()
    override fun getInitializers(): Array<UClassInitializer> = super.getInitializers()

    override fun getMethods(): Array<UMethod> {
        val hasPrimaryConstructor = ktClass?.hasPrimaryConstructor() ?: false
        var secondaryConstructorsCount = 0

        fun createUMethod(psiMethod: PsiMethod): UMethod {
            return if (psiMethod is KtLightMethod &&
                psiMethod.isConstructor) {
                if (!hasPrimaryConstructor && secondaryConstructorsCount++ == 0)
                    buildSecondaryConstructorUMethod(ktClass, psiMethod, this)
                else
                    buildPrimaryConstructorUMethod(ktClass, psiMethod, this)
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

    abstract fun buildPrimaryConstructorUMethod(ktClass: KtClassOrObject?, psi: KtLightMethod, givenParent: UElement?) : UMethod
    abstract fun buildSecondaryConstructorUMethod(ktClass: KtClassOrObject?, psi: KtLightMethod, givenParent: UElement?) : UMethod
}
