/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.util

import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue
import com.intellij.lang.jvm.annotation.JvmAnnotationEnumFieldValue
import com.intellij.psi.*
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.BreakingAbstractionKotlinLightMethodChecker
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.utilities.DokkaLogger

@InternalDokkaApi
internal class PsiHelper(
    private val logger: DokkaLogger,
    private val lightMethodChecker: BreakingAbstractionKotlinLightMethodChecker
) {
    /**
     * Extracts a list of [DRI]s for checked exceptions from a [PsiMethod]'s throws clause.
     */
    fun getCheckedExceptionDRIs(psiMethod: PsiMethod): List<DRI> =
        psiMethod.throwsList.toDriList()

    private fun PsiReferenceList.toDriList(): List<DRI> =
        referenceElements.mapNotNull { it?.resolve()?.let { resolved -> DRI.from(resolved) } }

    fun convertAnnotations(annotations: Collection<PsiAnnotation>): List<Annotations.Annotation> =
        annotations.filter { !lightMethodChecker.isLightAnnotation(it) }.mapNotNull { convertAnnotation(it) }

    private fun JvmAnnotationAttribute.toValue(): AnnotationParameterValue = when (this) {
        is PsiNameValuePair -> value?.toValue() ?: attributeValue?.toValue() ?: StringValue("")
        else -> StringValue(this.attributeName)
    }.let { annotationValue ->
        if (annotationValue is StringValue) annotationValue.copy(annotationValue.value.removeSurrounding("\""))
        else annotationValue
    }

    /**
     * This is a workaround for static imports from JDK like RetentionPolicy
     * For some reason they are not represented in the same way than using normal import
     */
    private fun JvmAnnotationAttributeValue.toValue(): AnnotationParameterValue? {
        return when (this) {
            is JvmAnnotationEnumFieldValue -> (field as? PsiElement)?.let { EnumValue(fieldName ?: "", DRI.from(it)) }
            // static import of a constant is resolved to constant value instead of a field/link
            is JvmAnnotationConstantValue -> this.constantValue?.toAnnotationLiteralValue()
            else -> null
        }
    }

    private fun Any.toAnnotationLiteralValue() = when (this) {
        is Byte -> IntValue(this.toInt())
        is Short -> IntValue(this.toInt())
        is Char -> StringValue(this.toString())
        is Int -> IntValue(this)
        is Long -> LongValue(this)
        is Boolean -> BooleanValue(this)
        is Float -> FloatValue(this)
        is Double -> DoubleValue(this)
        else -> StringValue(this.toString())
    }

    private fun PsiAnnotationMemberValue.toValue(): AnnotationParameterValue? = when (this) {
        is PsiAnnotation -> convertAnnotation(this)?.let { AnnotationValue(it) }
        is PsiArrayInitializerMemberValue -> ArrayValue(initializers.mapNotNull { it.toValue() })
        is PsiReferenceExpression -> psiReference?.let { EnumValue(text ?: "", DRI.from(it)) }
        is PsiClassObjectAccessExpression -> {
            val parameterType = (type as? PsiClassType)?.parameters?.firstOrNull()
            val classType = when (parameterType) {
                is PsiClassType -> parameterType.resolve()
                // Notice: Array<String>::class will be passed down as String::class
                // should probably be Array::class instead but this reflects behaviour for Kotlin sources
                is PsiArrayType -> (parameterType.componentType as? PsiClassType)?.resolve()
                else -> null
            }
            classType?.let { ClassValue(it.name ?: "", DRI.from(it)) }
        }

        is PsiLiteralExpression -> toValue()
        else -> StringValue(text ?: "")
    }

    private fun PsiLiteralExpression.toValue(): AnnotationParameterValue? = when (type) {
        PsiTypes.intType() -> (value as? Int)?.let { IntValue(it) }
        PsiTypes.longType() -> (value as? Long)?.let { LongValue(it) }
        PsiTypes.floatType() -> (value as? Float)?.let { FloatValue(it) }
        PsiTypes.doubleType() -> (value as? Double)?.let { DoubleValue(it) }
        PsiTypes.booleanType() -> (value as? Boolean)?.let { BooleanValue(it) }
        PsiTypes.nullType() -> NullValue
        else -> StringValue(text ?: "")
    }

    fun convertAnnotation(annotation: PsiAnnotation): Annotations.Annotation? {
        // TODO Mitigating workaround for issue https://github.com/Kotlin/dokka/issues/1341
        //  Tracking https://youtrack.jetbrains.com/issue/KT-41234
        //  Needs to be removed once this issue is fixed in light classes
        fun PsiElement.getAnnotationsOrNull(): Array<PsiAnnotation>? {
            this as PsiClass
            return try {
                this.annotations
            } catch (_: Exception) {
                logger.warn("Failed to get annotations from ${this.qualifiedName}")
                null
            }
        }

        return annotation.psiReference?.let { psiElement ->
            Annotations.Annotation(
                dri = DRI.from(psiElement),
                params = annotation.attributes
                    .filter { !lightMethodChecker.isLightAnnotationAttribute(it) }
                    .mapNotNull { it.attributeName to it.toValue() }
                    .toMap(),
                mustBeDocumented = psiElement.getAnnotationsOrNull().orEmpty().any { annotation ->
                    annotation.hasQualifiedName("java.lang.annotation.Documented")
                }
            )
        }
    }

    private val PsiElement.psiReference
        get() = getChildOfType<PsiJavaCodeReferenceElement>()?.resolve()

    fun getConstantExpression(field: PsiField): Expression? {
        val constantValue = field.computeConstantValue() ?: return null
        return when (constantValue) {
            is Byte -> IntegerConstant(constantValue.toLong())
            is Short -> IntegerConstant(constantValue.toLong())
            is Int -> IntegerConstant(constantValue.toLong())
            is Long -> IntegerConstant(constantValue)
            is Char -> StringConstant(constantValue.toString())
            is String -> StringConstant(constantValue)
            is Double -> DoubleConstant(constantValue)
            is Float -> FloatConstant(constantValue)
            is Boolean -> BooleanConstant(constantValue)
            else -> ComplexExpression(constantValue.toString())
        }
    }
}
