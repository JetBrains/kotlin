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

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.asJava.toLightGetter
import org.jetbrains.kotlin.asJava.toLightSetter
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.containingClassOrObject
import org.jetbrains.kotlin.psi.psiUtil.getStrictParentOfType
import org.jetbrains.kotlin.psi.psiUtil.parentsWithSelf
import org.jetbrains.uast.*
import org.jetbrains.uast.kotlin.expressions.KotlinUElvisExpression
import org.jetbrains.uast.kotlin.psi.UastKotlinPsiVariable

abstract class KotlinAbstractUElement(private val givenParent: UElement?) : UElement, JvmDeclarationUElement {

    override val uastParent: UElement? by lz {
        givenParent ?: convertParent()
    }

    protected open fun convertParent(): UElement? {
        val psi = psi
        var parent = psi?.parent ?: psi?.containingFile

        if (psi is KtAnnotationEntry) {
            val parentUnwrapped = KotlinConverter.unwrapElements(parent) ?: return null
            val target = psi.useSiteTarget?.getAnnotationUseSiteTarget()
            when (target) {
                AnnotationUseSiteTarget.PROPERTY_GETTER ->
                    parent = (parentUnwrapped as? KtProperty)?.getter
                             ?: (parentUnwrapped as? KtParameter)?.toLightGetter()
                             ?: parent

                AnnotationUseSiteTarget.PROPERTY_SETTER ->
                    parent = (parentUnwrapped as? KtProperty)?.setter
                             ?: (parentUnwrapped as? KtParameter)?.toLightSetter()
                             ?: parent
            }
        }
        if (psi is UastKotlinPsiVariable && parent != null) {
            parent = parent.parent
        }

        while (parent is KtStringTemplateEntryWithExpression ||
               parent is KtStringTemplateExpression && parent.entries.size == 1) {
            parent = parent.parent
        }

        if (parent is KtWhenConditionWithExpression) {
            parent = parent.parent
        }

        if (parent is KtImportList) {
            parent = parent.parent
        }

        if (psi is KtFunctionLiteral && parent is KtLambdaExpression) {
            parent = parent.parent
        }

        if (parent is KtLambdaArgument) {
            parent = parent.parent
        }

        if (psi is KtSuperTypeCallEntry) {
            parent = parent?.parent
        }

        val result = doConvertParent(this, parent)
        if (result == this) {
            throw IllegalStateException("Loop in parent structure when converting a $psi of type ${psi?.javaClass} with parent $parent of type ${parent?.javaClass} text: [${parent?.text}]")
        }

        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other !is UElement) {
            return false
        }

        return this.psi == other.psi
    }

    override fun hashCode() = psi?.hashCode() ?: 0
}

fun doConvertParent(element: UElement, parent: PsiElement?): UElement? {
    val parentUnwrapped = KotlinConverter.unwrapElements(parent) ?: return null
    if (parent is KtValueArgument && parentUnwrapped is KtAnnotationEntry) {
        return (KotlinUastLanguagePlugin().convertElementWithParent(parentUnwrapped, null) as? KotlinUAnnotation)
            ?.findAttributeValueExpression(parent)
    }

    if (parent is KtParameter) {
        val annotationClass = findAnnotationClassFromConstructorParameter(parent)
        if (annotationClass != null) {
            return annotationClass.methods.find { it.name == parent.name }
        }
    }

    if (parent is KtClassInitializer) {
        val containingClass = parent.containingClassOrObject
        if (containingClass != null) {
            val containingUClass = KotlinUastLanguagePlugin().convertElementWithParent(containingClass, null) as? KotlinUClass
            containingUClass?.methods?.filterIsInstance<KotlinConstructorUMethod>()?.firstOrNull { it.isPrimary }?.let {
                return it.uastBody
            }
        }
    }

    val result = KotlinUastLanguagePlugin().convertElementWithParent(parentUnwrapped, null)

    if (result is UEnumConstant && element is UDeclaration) {
        return result.initializingClass
    }

    if (result is USwitchClauseExpressionWithBody && !isInConditionBranch(element, result)) {
        return result.body
    }

    if (result is KotlinUDestructuringDeclarationExpression &&
        element.psi == (parent as KtDestructuringDeclaration).initializer) {
        return result.tempVarAssignment
    }

    if (result is KotlinUElvisExpression && parent is KtBinaryExpression) {
        when (element.psi) {
            parent.left -> return result.lhsDeclaration
            parent.right -> return result.rhsIfExpression
        }
    }

    return result
}

private fun isInConditionBranch(element: UElement, result: USwitchClauseExpressionWithBody) =
        element.psi?.parentsWithSelf?.takeWhile { it !== result.psi }?.any { it is KtWhenCondition } ?: false


private fun findAnnotationClassFromConstructorParameter(parameter: KtParameter): UClass? {
    val primaryConstructor = parameter.getStrictParentOfType<KtPrimaryConstructor>() ?: return null
    val containingClass = primaryConstructor.getContainingClassOrObject()
    if (containingClass.isAnnotation()) {
        return KotlinUastLanguagePlugin().convertElementWithParent(containingClass, null) as? UClass
    }
    return null
}

abstract class KotlinAbstractUExpression(givenParent: UElement?)
    : KotlinAbstractUElement(givenParent), UExpression, JvmDeclarationUElement {

    override val javaPsi = null
    override val sourcePsi
        get() = psi

    override val annotations: List<UAnnotation>
        get() {
            val annotatedExpression = psi?.parent as? KtAnnotatedExpression ?: return emptyList()
            return annotatedExpression.annotationEntries.map { KotlinUAnnotation(it, this) }
        }
}

