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
import org.jetbrains.kotlin.java.model.types.toJeType
import javax.lang.model.element.AnnotationValue
import javax.lang.model.element.AnnotationValueVisitor

fun JeAnnotationValue(psi: PsiAnnotationMemberValue): AnnotationValue = when (psi) {
    is PsiLiteral -> JeLiteralAnnotationValue(psi)
    is PsiAnnotation -> JeAnnotationAnnotationValue(psi)
    is PsiArrayInitializerMemberValue -> JeArrayAnnotationValue(psi)
    is PsiClassObjectAccessExpression -> JeTypeAnnotationValue(psi)
    is PsiReferenceExpression -> {
        // TODO check static final field -> primitive, reference to enum value
        val element = psi.resolve()
        if (element is PsiEnumConstant) {
            JeEnumValueAnnotationValue(element)
        }
        else {
            JeErrorAnnotationValue(psi)
        }
    }
    is PsiExpression -> JeExpressionAnnotationValue(psi)
    else -> throw AssertionError("Unsupported annotation element value: $psi")
}

private class JeAnnotationAnnotationValue(val psi: PsiAnnotation) : AnnotationValue {
    override fun getValue() = JeAnnotationMirror(psi)
    override fun <R : Any?, P : Any?> accept(v: AnnotationValueVisitor<R, P>, p: P) = v.visitAnnotation(value, p)
}

private class JeEnumValueAnnotationValue(val psi: PsiEnumConstant) : AnnotationValue {
    override fun getValue() = JeVariableElement(psi)
    override fun <R : Any?, P : Any?> accept(v: AnnotationValueVisitor<R, P>, p: P) = v.visitEnumConstant(value, p) 
}

private class JeTypeAnnotationValue(val psi: PsiClassObjectAccessExpression) : AnnotationValue {
    override fun getValue() = psi.operand.type.toJeType(psi.manager)
    override fun <R : Any?, P : Any?> accept(v: AnnotationValueVisitor<R, P>, p: P) = v.visitType(value, p)
}

private abstract class JePrimitiveAnnotationValue : AnnotationValue {
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

private class JeLiteralAnnotationValue(val psi: PsiLiteral) : JePrimitiveAnnotationValue() {
    override fun getValue() = psi.value
}

private class JeExpressionAnnotationValue(val psi: PsiExpression) : JePrimitiveAnnotationValue() {
    override fun getValue() = JavaPsiFacade.getInstance(psi.project).constantEvaluationHelper.computeConstantExpression(psi)
}

private class JeArrayAnnotationValue(val psi: PsiArrayInitializerMemberValue) : AnnotationValue {
    override fun getValue() = psi.initializers.map { JeAnnotationValue(it) }
    override fun <R : Any?, P : Any?> accept(v: AnnotationValueVisitor<R, P>, p: P) = v.visitArray(value, p)
}

private class JeErrorAnnotationValue(val psi: PsiElement) : AnnotationValue {
    override fun <R : Any?, P : Any?> accept(v: AnnotationValueVisitor<R, P>, p: P) = v.visitString(psi.text, p)
    override fun getValue() = null
}