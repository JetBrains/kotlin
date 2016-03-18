/*
 * Copyright 2000-2016 JetBrains s.r.o.
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
package org.jetbrains.uast.java

import com.intellij.psi.PsiArrayInitializerExpression
import com.intellij.psi.PsiArrayInitializerMemberValue
import com.intellij.psi.PsiMethodCallExpression
import com.intellij.psi.PsiNewExpression
import org.jetbrains.uast.*
import org.jetbrains.uast.psi.PsiElementBacked

class JavaUCallExpression(
        override val psi: PsiMethodCallExpression,
        override val parent: UElement
) : JavaAbstractUElement(), UCallExpression, PsiElementBacked, JavaTypeHelper, NoEvaluate {
    override val kind: UastCallKind
        get() = UastCallKind.FUNCTION_CALL

    override val functionReference by lz {
        JavaConverter.convert(psi.methodExpression.referenceNameElement, this) as? USimpleReferenceExpression
    }

    override val classReference: USimpleReferenceExpression?
        get() = null

    override val valueArgumentCount by lz { psi.argumentList.expressions.size }
    override val valueArguments by lz { psi.argumentList.expressions.map { JavaConverter.convert(it, this) } }

    override val typeArgumentCount by lz { psi.typeArguments.size }
    override val typeArguments by lz { psi.typeArguments.map { JavaConverter.convert(it, this) } }

    override val functionName: String
        get() = psi.methodExpression.referenceName ?: "<error name>"

    override val functionNameElement: UElement?
        get() = functionReference

    override fun resolve(context: UastContext) = psi.resolveMethod()?.let { context.convert(it) as? UFunction }
}

class JavaConstructorUCallExpression(
        override val psi: PsiNewExpression,
        override val parent: UElement
) : JavaAbstractUElement(), UCallExpression, PsiElementBacked, JavaTypeHelper, NoEvaluate {
    override val kind by lz {
        when {
            psi.arrayInitializer != null -> JavaUastCallKinds.ARRAY_INITIALIZER
            psi.arrayDimensions.isNotEmpty() -> JavaUastCallKinds.ARRAY_DIMENSIONS
            else -> UastCallKind.CONSTRUCTOR_CALL
        }
    }

    override val functionReference: USimpleReferenceExpression?
        get() = null

    override val classReference by lz {
        psi.classReference?.let { ref ->
            JavaClassUSimpleReferenceExpression(ref.element?.text.orAnonymous(), ref, this)
        }
    }

    override val valueArgumentCount: Int
        get() {
            val initializer = psi.arrayInitializer
            return if (initializer != null) {
                initializer.initializers.size
            } else if (psi.arrayDimensions.isNotEmpty()) {
                psi.arrayDimensions.size
            } else {
                psi.argumentList?.expressions?.size ?: 0
            }
        }

    override val valueArguments by lz {
        val initializer = psi.arrayInitializer
        if (initializer != null) {
            initializer.initializers.map { JavaConverter.convert(it, this) }
        }
        else if (psi.arrayDimensions.isNotEmpty()) {
            psi.arrayDimensions.map { JavaConverter.convert(it, this) }
        }
        else {
            psi.argumentList?.expressions?.map { JavaConverter.convert(it, this) } ?: emptyList()
        }
    }

    override val typeArgumentCount by lz { psi.typeArguments.size }
    override val typeArguments by lz { psi.typeArguments.map { JavaConverter.convert(it, this) } }

    override val functionName: String?
        get() {
            val initializer = psi.arrayInitializer
            return if (initializer != null)
                "<newArray>"
            else if (psi.arrayDimensions.isNotEmpty())
                "<newArrayWithDimensions>"
            else null
        }

    override val functionNameElement by lz { JavaDumbUElement(psi, this) }

    override fun resolve(context: UastContext) = psi.resolveConstructor()?.let { context.convert(it) } as? UFunction
}

class JavaArrayInitializerUCallExpression(
        override val psi: PsiArrayInitializerExpression,
        override val parent: UElement
) : JavaAbstractUElement(), UCallExpression, PsiElementBacked, JavaTypeHelper, JavaEvaluateHelper {
    override val functionReference: USimpleReferenceExpression?
        get() = null

    override val classReference: USimpleReferenceExpression?
        get() = null

    override val functionName: String
        get() = "<array>"

    override val functionNameElement: UElement?
        get() = null

    override val valueArgumentCount by lz { psi.initializers.size }
    override val valueArguments by lz { psi.initializers.map { JavaConverter.convert(it, this) } }

    override val typeArgumentCount: Int
        get() = 0

    override val typeArguments: List<UType>
        get() = emptyList()

    override val kind: UastCallKind
        get() = JavaUastCallKinds.ARRAY_INITIALIZER

    override fun resolve(context: UastContext) = null
    override fun evaluate() = null
}

class JavaAnnotationArrayInitializerUCallExpression(
        override val psi: PsiArrayInitializerMemberValue,
        override val parent: UElement
) : JavaAbstractUElement(), UCallExpression, PsiElementBacked, JavaTypeHelper, JavaEvaluateHelper {
    override val kind = JavaUastCallKinds.ARRAY_INITIALIZER

    override val functionReference: USimpleReferenceExpression?
        get() = null

    override val classReference: USimpleReferenceExpression?
        get() = null

    override val functionName: String
        get() = "<annotationArray>"

    override val functionNameElement: UElement?
        get() = null

    override val valueArgumentCount by lz { psi.initializers.size }
    override val valueArguments by lz {
        psi.initializers.map {
            JavaConverter.convert(it, this) as? UExpression ?: UnknownJavaExpression(it, this)
        }
    }

    override val typeArgumentCount: Int
        get() = 0

    override val typeArguments: List<UType>
        get() = emptyList()

    override fun resolve(context: UastContext) = null
}