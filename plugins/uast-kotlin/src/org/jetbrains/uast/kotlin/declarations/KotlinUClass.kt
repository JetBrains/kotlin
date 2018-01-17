/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.uast.kotlin

import com.intellij.psi.*
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.KtLightClassForLocalDeclaration
import org.jetbrains.kotlin.asJava.classes.KtLightClassForScript
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.kotlin.utils.addToStdlib.firstIsInstanceOrNull
import org.jetbrains.uast.*
import org.jetbrains.uast.java.AbstractJavaUClass
import org.jetbrains.uast.kotlin.declarations.KotlinUMethod
import org.jetbrains.uast.kotlin.declarations.UastLightIdentifier

abstract class AbstractKotlinUClass(private val givenParent: UElement?) : AbstractJavaUClass() {
    override val uastParent: UElement? by lz { givenParent ?: convertParent() }

    //TODO: should be merged with KotlinAbstractUElement.convertParent() after detaching from AbstractJavaUClass
    open fun convertParent(): UElement? =
            (this.psi as? KtLightClassForLocalDeclaration)?.kotlinOrigin?.parent?.let {
                when (it) {
                    is KtClassBody -> it.parent.toUElement() // TODO: it seems that `class_body`-s are never created in kotlin uast in top-down walk, probably they should be completely skipped and always unwrapped
                    else -> it.toUElement()
                }
            } ?: (psi.parent ?: psi.containingFile).toUElement()

}

open class KotlinUClass private constructor(
        psi: KtLightClass,
        givenParent: UElement?
) : AbstractKotlinUClass(givenParent), PsiClass by psi {

    val ktClass = psi.kotlinOrigin

    override val psi = unwrap<UClass, PsiClass>(psi)

    override fun getOriginalElement(): PsiElement? = super.getOriginalElement()

    override fun getNameIdentifier(): PsiIdentifier? = UastLightIdentifier(psi, ktClass)

    override fun getContainingFile(): PsiFile? = unwrapFakeFileForLightClass(psi.containingFile)

    override val annotations: List<UAnnotation>
        get() = ktClass?.annotationEntries?.map { KotlinUAnnotation(it, this) } ?: emptyList()

    override val uastAnchor: UElement
        get() = UIdentifier(nameIdentifier, this)

    override fun getInnerClasses(): Array<UClass> {
        // filter DefaultImpls to avoid processing same methods from original interface multiple times
        // filter Enum entry classes to avoid duplication with PsiEnumConstant initializer class
        return psi.innerClasses.filter {
            it.name != JvmAbi.DEFAULT_IMPLS_CLASS_NAME && !it.isEnumEntryLightClass()
        }.mapNotNull {
            getLanguagePlugin().convertOpt<UClass>(it, this)
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
                    KotlinSecondaryConstructorWithInitializersUMethod(ktClass, psiMethod, this)
                else
                    KotlinConstructorUMethod(ktClass, psiMethod, this)
            } else {
                getLanguagePlugin().convert(psiMethod, this)
            }
        }

        fun isDelegatedMethod(psiMethod: PsiMethod) = psiMethod is KtLightMethod && psiMethod.isDelegated

        return psi.methods.asSequence()
                .filterNot(::isDelegatedMethod)
                .map(::createUMethod)
                .toList()
                .toTypedArray()
    }

    private fun PsiClass.isEnumEntryLightClass() = (this as? KtLightClass)?.kotlinOrigin is KtEnumEntry

    companion object {
        fun create(psi: KtLightClass, containingElement: UElement?): UClass = when (psi) {
            is PsiAnonymousClass -> KotlinUAnonymousClass(psi, containingElement)
            is KtLightClassForScript -> KotlinScriptUClass(psi, containingElement)
            else -> KotlinUClass(psi, containingElement)
        }
    }
}

open class KotlinConstructorUMethod(
        private val ktClass: KtClassOrObject?,
        override val psi: KtLightMethod,
        givenParent: UElement?
) : KotlinUMethod(psi, givenParent) {

    val isPrimary: Boolean
        get() = psi.kotlinOrigin.let { it is KtPrimaryConstructor || it is KtClassOrObject }

    override val uastBody: UExpression? by lz {
        val delegationCall: KtCallElement? = psi.kotlinOrigin.let {
            when {
                isPrimary -> ktClass?.superTypeListEntries?.firstIsInstanceOrNull<KtSuperTypeCallEntry>()
                it is KtSecondaryConstructor -> it.getDelegationCall()
                else -> null
            }
        }
        val bodyExpressions = getBodyExpressions()
        if (delegationCall == null && bodyExpressions.isEmpty()) return@lz null
        KotlinUBlockExpression.KotlinLazyUBlockExpression(this) { uastParent ->
            SmartList<UExpression>().apply {
                delegationCall?.let {
                    add(KotlinUFunctionCallExpression(it, uastParent))
                }
                bodyExpressions.forEach {
                    add(KotlinConverter.convertOrEmpty(it, uastParent))
                }
            }
        }
    }

    open protected fun getBodyExpressions(): List<KtExpression> {
        if (isPrimary) return getInitializers()
        val bodyExpression = (psi.kotlinOrigin as? KtFunction)?.bodyExpression ?: return emptyList()
        if (bodyExpression is KtBlockExpression) return bodyExpression.statements
        return listOf(bodyExpression)
    }

    protected fun getInitializers() = ktClass?.getAnonymousInitializers()?.mapNotNull { it.body } ?: emptyList()

}

// This class was created as a workaround for KT-21617 to be the only constructor which includes `init` block
// when there is no primary constructors in the class.
// It is expected to have only one constructor of this type in a UClass.
class KotlinSecondaryConstructorWithInitializersUMethod(
        ktClass: KtClassOrObject?,
        psi: KtLightMethod,
        givenParent: UElement?
) : KotlinConstructorUMethod(ktClass, psi, givenParent) {
    override fun getBodyExpressions(): List<KtExpression> = getInitializers() + super.getBodyExpressions()
}

class KotlinUAnonymousClass(
        psi: PsiAnonymousClass,
        givenParent: UElement?
) : AbstractKotlinUClass(givenParent), UAnonymousClass, PsiAnonymousClass by psi {

    override val psi: PsiAnonymousClass = unwrap<UAnonymousClass, PsiAnonymousClass>(psi)

    override fun getOriginalElement(): PsiElement? = super<AbstractKotlinUClass>.getOriginalElement()

    override fun getSuperClass(): UClass? = super<AbstractKotlinUClass>.getSuperClass()
    override fun getFields(): Array<UField> = super<AbstractKotlinUClass>.getFields()
    override fun getMethods(): Array<UMethod> = super<AbstractKotlinUClass>.getMethods()
    override fun getInitializers(): Array<UClassInitializer> = super<AbstractKotlinUClass>.getInitializers()
    override fun getInnerClasses(): Array<UClass> = super<AbstractKotlinUClass>.getInnerClasses()

    override fun getContainingFile(): PsiFile = unwrapFakeFileForLightClass(psi.containingFile)

    override val uastAnchor: UElement?
        get() {
            val ktClassOrObject = (psi.originalElement as? KtLightClass)?.kotlinOrigin as? KtObjectDeclaration ?: return null
            return UIdentifier(ktClassOrObject.getObjectKeyword(), this)
        }

}

class KotlinScriptUClass(
        psi: KtLightClassForScript,
        override val uastParent: UElement?
) : AbstractJavaUClass(), PsiClass by psi {
    override fun getContainingFile(): PsiFile = unwrapFakeFileForLightClass(psi.containingFile)

    override fun getNameIdentifier(): PsiIdentifier? = UastLightIdentifier(psi, psi.kotlinOrigin)

    override val uastAnchor: UElement
        get() = UIdentifier(nameIdentifier, this)

    override val psi = unwrap<UClass, KtLightClassForScript>(psi)

    override fun getSuperClass(): UClass? = super.getSuperClass()

    override fun getFields(): Array<UField> = super.getFields()

    override fun getInitializers(): Array<UClassInitializer> = super.getInitializers()

    override fun getInnerClasses(): Array<UClass> =
            psi.innerClasses.mapNotNull { getLanguagePlugin().convertOpt<UClass>(it, this) }.toTypedArray()

    override fun getMethods(): Array<UMethod> = psi.methods.map(this::createUMethod).toTypedArray()

    private fun createUMethod(method: PsiMethod): UMethod {
        return if (method.isConstructor) {
            KotlinScriptConstructorUMethod(psi.script, method as KtLightMethod, this)
        }
        else {
            getLanguagePlugin().convert(method, this)
        }
    }

    override fun getOriginalElement(): PsiElement? = psi.originalElement

    class KotlinScriptConstructorUMethod(
            script: KtScript,
            override val psi: KtLightMethod,
            override val uastParent: UElement?
    ) : KotlinUMethod(psi, uastParent) {
        override val uastBody: UExpression? by lz {
            val initializers = script.declarations.filterIsInstance<KtScriptInitializer>()
            KotlinUBlockExpression.create(initializers, this)
        }
    }
}