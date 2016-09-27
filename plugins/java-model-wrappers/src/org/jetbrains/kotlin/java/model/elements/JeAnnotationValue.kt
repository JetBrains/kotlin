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

package org.jetbrains.kotlin.java.model.elements

import com.intellij.psi.*
import org.jetbrains.kotlin.java.model.JeDisposablePsiElementOwner
import org.jetbrains.kotlin.java.model.internal.calcConstantValue
import org.jetbrains.kotlin.java.model.types.toJeType
import java.util.*
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.AnnotationValueVisitor

fun JeAnnotationValue(psi: PsiAnnotationMemberValue): AnnotationValue {
    val original = psi.originalElement
    val annotationValue = when (original) {
        is PsiLiteral -> JeLiteralAnnotationValue(original)
        is PsiAnnotation -> JeAnnotationAnnotationValue(original)
        is PsiArrayInitializerMemberValue -> JeArrayAnnotationValue(original)
        is PsiClassObjectAccessExpression -> JeTypeAnnotationValue(original)
        is PsiReferenceExpression -> {
            val element = original.resolve()
            if (element is PsiEnumConstant) {
                JeEnumValueAnnotationValue(element)
            } 
            else if (element is PsiField && element.hasInitializer()) {
                JeAnnotationValue(element.initializer ?: error("Field should have an initializer"))
            }
            else {
                JeErrorAnnotationValue(psi)
            }
        }
        is PsiExpression -> JeExpressionAnnotationValue(original)
        else -> throw AssertionError("Unsupported annotation element value: $psi (original = $original)")
    }
    return annotationValue
}

class JeAnnotationAnnotationValue(psi: PsiAnnotation) : JeDisposablePsiElementOwner<PsiAnnotation>(psi), AnnotationValue {
    override fun getValue() = JeAnnotationMirror(psi)
    override fun <R : Any?, P : Any?> accept(v: AnnotationValueVisitor<R, P>, p: P) = v.visitAnnotation(value, p)
}

class JeEnumValueAnnotationValue(psi: PsiEnumConstant) : JeDisposablePsiElementOwner<PsiEnumConstant>(psi), AnnotationValue {
    override fun getValue() = JeVariableElement(psi)
    override fun <R : Any?, P : Any?> accept(v: AnnotationValueVisitor<R, P>, p: P) = v.visitEnumConstant(value, p) 
}

class JeTypeAnnotationValue(psi: PsiClassObjectAccessExpression) : JeDisposablePsiElementOwner<PsiClassObjectAccessExpression>(psi), AnnotationValue {
    override fun getValue() = psi.operand.type.toJeType(psi.manager)
    override fun <R : Any?, P : Any?> accept(v: AnnotationValueVisitor<R, P>, p: P) = v.visitType(value, p)
}

abstract class JePrimitiveAnnotationValue<out T : PsiElement>(psi: T) : JeDisposablePsiElementOwner<T>(psi), AnnotationValue {
    override fun <R : Any?, P : Any?> accept(v: AnnotationValueVisitor<R, P>, p: P): R {
        val value = this.value
        return when (value) {
            is String -> v.visitString(value, p)
            is Int -> v.visitInt(value, p)
            is Boolean -> v.visitBoolean(value, p)
            is Char -> v.visitChar(value, p)
            is Byte -> v.visitByte(value, p)
            is Short -> v.visitShort(value, p)
            is Long -> v.visitLong(value, p)
            is Float -> v.visitFloat(value, p)
            is Double -> v.visitDouble(value, p)
            else -> throw AssertionError("Bad annotation element value: $value")
        }
    }
}

class JeLiteralAnnotationValue(psi: PsiLiteral) : JePrimitiveAnnotationValue<PsiLiteral>(psi) {
    override fun getValue() = psi.value
}

class JeExpressionAnnotationValue(psi: PsiExpression) : JePrimitiveAnnotationValue<PsiExpression>(psi) {
    override fun getValue() = psi.calcConstantValue()
}

class JeArrayAnnotationValue(psi: PsiArrayInitializerMemberValue) : JeDisposablePsiElementOwner<PsiArrayInitializerMemberValue>(psi), AnnotationValue {
    override fun getValue() = psi.initializers.map(::JeAnnotationValue)
    override fun <R : Any?, P : Any?> accept(v: AnnotationValueVisitor<R, P>, p: P) = v.visitArray(value, p)
}

class JeSingletonArrayAnnotationValue(psi: PsiAnnotationMemberValue) : JeDisposablePsiElementOwner<PsiAnnotationMemberValue>(psi), AnnotationValue {
    override fun getValue(): List<AnnotationValue> = Collections.singletonList(JeAnnotationValue(psi))
    override fun <R : Any?, P : Any?> accept(v: AnnotationValueVisitor<R, P>, p: P) = v.visitArray(value, p)
}

class JeErrorAnnotationValue(psi: PsiElement) : JeDisposablePsiElementOwner<PsiElement>(psi), AnnotationValue {
    override fun <R : Any?, P : Any?> accept(v: AnnotationValueVisitor<R, P>, p: P) = v.visitString(psi.text, p)
    override fun getValue() = null
}