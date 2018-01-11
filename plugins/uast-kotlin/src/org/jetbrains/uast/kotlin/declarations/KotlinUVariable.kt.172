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
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.elements.KtLightElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.resolve.calls.callUtil.getType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.typeUtil.TypeNullability
import org.jetbrains.kotlin.types.typeUtil.nullability
import org.jetbrains.kotlin.utils.SmartList
import org.jetbrains.uast.*
import org.jetbrains.uast.internal.acceptList
import org.jetbrains.uast.java.JavaAbstractUExpression
import org.jetbrains.uast.kotlin.declarations.UastLightIdentifier
import org.jetbrains.uast.kotlin.internal.KotlinUElementWithComments
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiParameter
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiVariable
import org.jetbrains.uast.visitor.UastVisitor

abstract class AbstractKotlinUVariable(givenParent: UElement?)
    : KotlinAbstractUElement(givenParent), PsiVariable, UVariable, KotlinUElementWithComments {

    override val uastInitializer: UExpression?
        get() {
            val psi = psi
            val initializerExpression = when (psi) {
                is UastKotlinPsiVariable -> psi.ktInitializer
                is UastKotlinPsiParameter -> psi.ktDefaultValue
                is KtLightElement<*, *> -> {
                    val origin = psi.kotlinOrigin
                    when (origin) {
                        is KtVariableDeclaration -> origin.initializer
                        is KtParameter -> origin.defaultValue
                        else -> null
                    }
                }
                else -> null
            } ?: return null
            return getLanguagePlugin().convertElement(initializerExpression, this) as? UExpression ?: UastEmptyExpression
        }

    val delegateExpression: UExpression? by lz {
        val psi = psi
        val expression = when (psi) {
            is KtLightElement<*, *> -> (psi.kotlinOrigin as? KtProperty)?.delegateExpression
            is UastKotlinPsiVariable -> (psi.ktElement as? KtProperty)?.delegateExpression
            else -> null
        }

        expression?.let { getLanguagePlugin().convertElement(it, this) as? UExpression }
    }

    override fun getNameIdentifier(): PsiIdentifier {
        val kotlinOrigin = (psi as? KtLightElement<*, *>)?.kotlinOrigin
        return UastLightIdentifier(psi, kotlinOrigin as KtNamedDeclaration?)
    }

    override fun getContainingFile(): PsiFile = unwrapFakeFileForLightClass(psi.containingFile)

    override val annotations by lz {
        val sourcePsi = sourcePsi ?: return@lz psi.annotations.map { WrappedUAnnotation(it, this) }
        val annotations = SmartList<UAnnotation>(KotlinNullabilityUAnnotation(sourcePsi, this))
        if (sourcePsi is KtModifierListOwner) {
            sourcePsi.annotationEntries.
                    filter { acceptsAnnotationTarget(it.useSiteTarget?.getAnnotationUseSiteTarget()) }.
                    mapTo(annotations) { KotlinUAnnotation(it, this) }
        }
        annotations
    }


    abstract protected fun acceptsAnnotationTarget(target: AnnotationUseSiteTarget?): Boolean

    override val typeReference by lz { getLanguagePlugin().convertOpt<UTypeReferenceExpression>(psi.typeElement, this) }

    override val uastAnchor: UElement?
        get() = UIdentifier(nameIdentifier, this)

    override fun equals(other: Any?) = other is AbstractKotlinUVariable && psi == other.psi

    class WrappedUAnnotation(psiAnnotation: PsiAnnotation, override val uastParent: UElement) : UAnnotation, JvmDeclarationUElement {

        override val javaPsi: PsiAnnotation = psiAnnotation
        override val psi: PsiAnnotation = javaPsi
        override val sourcePsi: PsiElement? = null

        override val attributeValues: List<UNamedExpression> by lz {
            psi.parameterList.attributes.map { WrappedUNamedExpression(it, this) }
        }

        class WrappedUNamedExpression(pair: PsiNameValuePair, override val uastParent: UElement?) : UNamedExpression, JvmDeclarationUElement {
            override val name: String? = pair.name
            override val psi = pair
            override val javaPsi: PsiElement? = psi
            override val sourcePsi: PsiElement? = null
            override val annotations: List<UAnnotation> = emptyList()
            override val expression: UExpression by lz { toUExpression(psi.value) }
        }

        override val qualifiedName: String? = psi.qualifiedName
        override fun findAttributeValue(name: String?): UExpression? = psi.findAttributeValue(name)?.let { toUExpression(it) }
        override fun findDeclaredAttributeValue(name: String?): UExpression? = psi.findDeclaredAttributeValue(name)?.let { toUExpression(it) }
        override fun resolve(): PsiClass? = psi.nameReferenceElement?.resolve() as? PsiClass
    }

}

private fun toUExpression(psi: PsiElement?): UExpression = psi.toUElementOfType<UExpression>() ?: UastEmptyExpression

class KotlinUVariable(
        psi: PsiVariable,
        override val sourcePsi: KtElement,
        givenParent: UElement?
) : AbstractKotlinUVariable(givenParent), UVariable, PsiVariable by psi {

    override val javaPsi = unwrap<UVariable, PsiVariable>(psi)

    override val psi = javaPsi

    override val typeReference by lz { getLanguagePlugin().convertOpt<UTypeReferenceExpression>(psi.typeElement, this) }

    override fun acceptsAnnotationTarget(target: AnnotationUseSiteTarget?): Boolean = true

    override fun getInitializer(): PsiExpression? {
        return super<AbstractKotlinUVariable>.getInitializer()
    }

    override fun getOriginalElement(): PsiElement? {
        return super<AbstractKotlinUVariable>.getOriginalElement()
    }

    override fun getNameIdentifier(): PsiIdentifier {
        return super.getNameIdentifier()
    }

    override fun getContainingFile(): PsiFile {
        return super.getContainingFile()
    }

}

open class KotlinUParameter(
        psi: PsiParameter,
        final override val sourcePsi: KtElement?,
        givenParent: UElement?
) : AbstractKotlinUVariable(givenParent), UParameter, PsiParameter by psi {

    final override val javaPsi = unwrap<UParameter, PsiParameter>(psi)

    override val psi = javaPsi

    private val isLightConstructorParam by lz { psi.getParentOfType<PsiMethod>(true)?.isConstructor }

    private val isKtConstructorParam by lz { sourcePsi?.getParentOfType<KtCallableDeclaration>(true)?.let { it is KtConstructor<*> } }

    override fun acceptsAnnotationTarget(target: AnnotationUseSiteTarget?): Boolean {
        if (sourcePsi !is KtParameter) return false
        if (isKtConstructorParam == isLightConstructorParam && target == null) return true
        when (target) {
            AnnotationUseSiteTarget.CONSTRUCTOR_PARAMETER -> return isLightConstructorParam == true
            AnnotationUseSiteTarget.SETTER_PARAMETER -> return isLightConstructorParam != true
            else -> return false
        }
    }

    override fun getInitializer(): PsiExpression? {
        return super<AbstractKotlinUVariable>.getInitializer()
    }

    override fun getOriginalElement(): PsiElement? {
        return super<AbstractKotlinUVariable>.getOriginalElement()
    }

    override fun getNameIdentifier(): PsiIdentifier {
        return super.getNameIdentifier()
    }

    override fun getContainingFile(): PsiFile {
        return super.getContainingFile()
    }
}

class KotlinNullabilityUAnnotation(val annotatedElement: PsiElement, override val uastParent: UElement) : UAnnotation, JvmDeclarationUElement {

    private fun getTargetType(annotatedElement: PsiElement): KotlinType? {
        if (annotatedElement is KtCallableDeclaration) {
            annotatedElement.typeReference?.getType()?.let { return it }
        }
        if (annotatedElement is KtProperty) {
            annotatedElement.initializer?.let { it.getType(it.analyze()) }?.let { return it }
            annotatedElement.delegateExpression?.let { it.getType(it.analyze())?.arguments?.firstOrNull()?.type }?.let { return it }
        }
        annotatedElement.getParentOfType<KtProperty>(false)?.let {
            it.typeReference?.getType() ?: it.initializer?.let { it.getType(it.analyze()) }
        }?.let { return it }
        return null
    }

    val nullability by lz { getTargetType(annotatedElement)?.nullability() }

    override val attributeValues: List<UNamedExpression>
        get() = emptyList()
    override val psi: PsiElement?
        get() = null
    override val javaPsi: PsiAnnotation?
        get() = null
    override val sourcePsi: PsiElement?
        get() = null
    override val qualifiedName: String?
        get() = when (nullability) {
            TypeNullability.NOT_NULL -> NotNull::class.qualifiedName
            TypeNullability.NULLABLE -> Nullable::class.qualifiedName
            TypeNullability.FLEXIBLE -> null
            null -> null
        }

    override fun findAttributeValue(name: String?): UExpression? = null

    override fun findDeclaredAttributeValue(name: String?): UExpression? = null

    override fun resolve(): PsiClass? = qualifiedName?.let {
        val project = annotatedElement.project
        JavaPsiFacade.getInstance(project).findClass(it, GlobalSearchScope.allScope(project))
    }

}

open class KotlinUField(
        psi: PsiField,
        override val sourcePsi: KtElement?,
        givenParent: UElement?
) : AbstractKotlinUVariable(givenParent), UField, PsiField by psi {
    override fun getSourceElement() = sourcePsi ?: this

    override val javaPsi  = unwrap<UField, PsiField>(psi)

    override val psi = javaPsi

    override fun acceptsAnnotationTarget(target: AnnotationUseSiteTarget?): Boolean =
            target == AnnotationUseSiteTarget.FIELD ||
            target == AnnotationUseSiteTarget.PROPERTY_DELEGATE_FIELD ||
            (sourcePsi is KtProperty) && (target == null || target == AnnotationUseSiteTarget.PROPERTY)

    override fun getInitializer(): PsiExpression? {
        return super<AbstractKotlinUVariable>.getInitializer()
    }

    override fun getOriginalElement(): PsiElement? {
        return super<AbstractKotlinUVariable>.getOriginalElement()
    }

    override fun getNameIdentifier(): PsiIdentifier {
        return super.getNameIdentifier()
    }

    override fun getContainingFile(): PsiFile {
        return super.getContainingFile()
    }

    override fun isPhysical(): Boolean {
        return true
    }

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitField(this)) return
        annotations.acceptList(visitor)
        uastInitializer?.accept(visitor)
        delegateExpression?.accept(visitor)
        visitor.afterVisitField(this)
    }
}

open class KotlinULocalVariable(
        psi: PsiLocalVariable,
        override val sourcePsi: KtElement,
        givenParent: UElement?
) : AbstractKotlinUVariable(givenParent), ULocalVariable, PsiLocalVariable by psi {

    override val javaPsi = unwrap<ULocalVariable, PsiLocalVariable>(psi)

    override fun acceptsAnnotationTarget(target: AnnotationUseSiteTarget?): Boolean = true

    override val psi = javaPsi

    override fun getInitializer(): PsiExpression? {
        return super<AbstractKotlinUVariable>.getInitializer()
    }

    override fun getOriginalElement(): PsiElement? {
        return super<AbstractKotlinUVariable>.getOriginalElement()
    }

    override fun getNameIdentifier(): PsiIdentifier {
        return super.getNameIdentifier()
    }

    override fun getContainingFile(): PsiFile {
        return super.getContainingFile()
    }

    override fun accept(visitor: UastVisitor) {
        if (visitor.visitLocalVariable(this)) return
        annotations.acceptList(visitor)
        uastInitializer?.accept(visitor)
        delegateExpression?.accept(visitor)
        visitor.afterVisitLocalVariable(this)
    }
}

open class KotlinUAnnotatedLocalVariable(
        psi: PsiLocalVariable,
        sourcePsi: KtElement,
        uastParent: UElement?,
        computeAnnotations: (parent: UElement) -> List<UAnnotation>
) : KotlinULocalVariable(psi, sourcePsi, uastParent) {

    override val annotations: List<UAnnotation> by lz { computeAnnotations(this) }
}

open class KotlinUEnumConstant(
        psi: PsiEnumConstant,
        override val sourcePsi: KtElement?,
        givenParent: UElement?
) : AbstractKotlinUVariable(givenParent), UEnumConstant, PsiEnumConstant by psi {

    override val initializingClass: UClass? by lz {
        (psi.initializingClass as? KtLightClass)?.let { initializingClass ->
            KotlinUClass.create(initializingClass, this)
        }
    }

    override val javaPsi = unwrap<UEnumConstant, PsiEnumConstant>(psi)

    override val psi = javaPsi

    override fun acceptsAnnotationTarget(target: AnnotationUseSiteTarget?): Boolean = true

    override fun getContainingFile(): PsiFile {
        return super.getContainingFile()
    }

    override fun getNameIdentifier(): PsiIdentifier {
        return super.getNameIdentifier()
    }

    override val kind: UastCallKind
        get() = UastCallKind.CONSTRUCTOR_CALL

    override val receiver: UExpression?
        get() = null

    override val receiverType: PsiType?
        get() = null

    override val methodIdentifier: UIdentifier?
        get() = null

    override val classReference: UReferenceExpression?
        get() = KotlinEnumConstantClassReference(psi, sourcePsi, this)

    override val typeArgumentCount: Int
        get() = 0

    override val typeArguments: List<PsiType>
        get() = emptyList()

    override val valueArgumentCount: Int
        get() = psi.argumentList?.expressions?.size ?: 0

    override val valueArguments by lz {
        psi.argumentList?.expressions?.map {
            getLanguagePlugin().convertElement(it, this) as? UExpression ?: UastEmptyExpression
        } ?: emptyList()
    }

    override val returnType: PsiType?
        get() = psi.type

    override fun resolve() = psi.resolveMethod()

    override val methodName: String?
        get() = null

    private class KotlinEnumConstantClassReference(
            override val psi: PsiEnumConstant,
            override val sourcePsi: KtElement?,
            private val givenParent: UElement?
    ) : JavaAbstractUExpression(), USimpleNameReferenceExpression {
        override val javaPsi: PsiElement?
            get() = psi

        override val uastParent: UElement? by lz {
            givenParent ?: KotlinUastLanguagePlugin().convertElementWithParent(psi.parent ?: psi.containingFile, null)
        }

        override fun resolve() = psi.containingClass
        override val resolvedName: String?
            get() = psi.containingClass?.name
        override val identifier: String
            get() = psi.containingClass?.name ?: "<error>"
    }
}