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

import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtValueArgument
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UElement
import org.jetbrains.uast.UExpression

abstract class KotlinAbstractUElement(private val givenParent: UElement?) : UElement {

    override val uastParent: UElement? by lz { convertParent(givenParent) }

    override fun equals(other: Any?): Boolean {
        if (other !is UElement) {
            return false
        }

        return this.psi == other.psi
    }

    override fun hashCode() = psi?.hashCode() ?: 0
}

internal fun UElement.convertParent(givenParent: UElement?): UElement? {
    return if (givenParent != null)
        givenParent
    else {
        val parent = psi?.parent
        val result = KotlinConverter.unwrapElements(parent)?.let { parentUnwrapped ->
            if (parent is KtValueArgument && parentUnwrapped is KtAnnotationEntry) {
                val argumentName = parent.getArgumentName()?.asName?.asString() ?: ""
                (KotlinUastLanguagePlugin().convertElementWithParent(parentUnwrapped, null) as? UAnnotation)
                    ?.attributeValues?.find { it.name == argumentName }
            }
            else
                KotlinUastLanguagePlugin().convertElementWithParent(parentUnwrapped, null)
        }
        if (result == this) {
            throw IllegalStateException("Loop in parent structure")
        }
        result
    }
}

abstract class KotlinAbstractUExpression(givenParent: UElement?)
    : KotlinAbstractUElement(givenParent), UExpression {

    override val annotations: List<UAnnotation>
        get() {
            val annotatedExpression = psi?.parent as? KtAnnotatedExpression ?: return emptyList()
            return annotatedExpression.annotationEntries.map { KotlinUAnnotation(it, this) }
        }
}