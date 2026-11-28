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
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.utilities.DokkaLogger
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.documentation.KdElementId

@InternalDokkaApi
internal class PsiHelper(
    private val logger: DokkaLogger,
    private val lightMethodChecker: BreakingAbstractionKotlinLightMethodChecker
) {
    /**
     * Extracts the declaration IDs of the checked exceptions from a [PsiMethod]'s throws clause.
     */
    context(_: KaSession)
    fun getCheckedExceptionDeclarationIds(psiMethod: PsiMethod): List<KdElementId> =
        psiMethod.throwsList.toDeclarationIdList()

    context(_: KaSession)
    private fun PsiReferenceList.toDeclarationIdList(): List<KdElementId> =
        referenceElements.mapNotNull { it?.resolve()?.let { resolved -> declarationIdFrom(resolved) } }

    context(_: KaSession)
    fun convertAnnotations(annotations: Collection<PsiAnnotation>): List<Annotations.Annotation> =
        annotations.filter { !lightMethodChecker.isLightAnnotation(it) }.mapNotNull { convertAnnotation(it) }

    context(_: KaSession)
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
    context(_: KaSession)
    private fun JvmAnnotationAttributeValue.toValue(): AnnotationParameterValue? {
        return when (this) {
            is JvmAnnotationEnumFieldValue -> (field as? PsiField)?.let { field ->
                declarationIdFrom(field)?.let { id -> EnumValue(fieldName ?: "", id) }
            }
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

    context(_: KaSession)
    private fun PsiAnnotationMemberValue.toValue(): AnnotationParameterValue? = when (this) {
        is PsiAnnotation -> convertAnnotation(this)?.let { AnnotationValue(it) }
        is PsiArrayInitializerMemberValue -> ArrayValue(initializers.mapNotNull { it.toValue() })
        // Resolve the whole expression: for a qualified reference like `ElementType.FIELD`,
        // the first code-reference child is the `ElementType` qualifier, which would resolve
        // to the enum class instead of the entry. Only enum entries become links; a static
        // import of a plain constant (e.g. `static final int BYTE = 3`) falls back to the
        // literal-value handling.
        is PsiReferenceExpression -> resolve()?.let { resolved ->
            (resolved as? PsiEnumConstant)?.let { field -> declarationIdFrom(field)?.let { id -> EnumValue(text ?: "", id) } }
        }
        is PsiClassObjectAccessExpression -> {
            val parameterType = (type as? PsiClassType)?.parameters?.firstOrNull()
            val classType = when (parameterType) {
                is PsiClassType -> parameterType.resolve()
                // Notice: Array<String>::class will be passed down as String::class
                // should probably be Array::class instead but this reflects behaviour for Kotlin sources
                is PsiArrayType -> (parameterType.componentType as? PsiClassType)?.resolve()
                else -> null
            }
            classType?.let { classType ->
                declarationIdFrom(classType)?.let { id -> ClassValue(classType.name ?: "", id) }
            }
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

    context(_: KaSession)
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
            declarationIdFrom(psiElement)?.let { classLikeId ->
                Annotations.Annotation(
                    classLikeId = classLikeId,
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
