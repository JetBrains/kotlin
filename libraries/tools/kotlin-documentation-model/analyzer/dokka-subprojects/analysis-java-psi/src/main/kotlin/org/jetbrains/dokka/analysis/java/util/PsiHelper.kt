/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.java.util

import com.intellij.lang.jvm.annotation.JvmAnnotationAttribute
import com.intellij.lang.jvm.annotation.JvmAnnotationAttributeValue
import com.intellij.lang.jvm.annotation.JvmAnnotationConstantValue
import com.intellij.lang.jvm.annotation.JvmAnnotationEnumFieldValue
import com.intellij.psi.*
import org.jetbrains.dokka.DokkaConfiguration
import org.jetbrains.dokka.InternalDokkaApi
import org.jetbrains.dokka.analysis.java.BreakingAbstractionKotlinLightMethodChecker
import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.AnnotationTarget
import org.jetbrains.dokka.model.properties.PropertyContainer
import org.jetbrains.dokka.utilities.DokkaLogger

@InternalDokkaApi
public class PsiHelper(
    private val sourceSet: DokkaConfiguration.DokkaSourceSet,
    private val logger: DokkaLogger,
    private val lightMethodChecker: BreakingAbstractionKotlinLightMethodChecker
) {
    private val cachedBounds = hashMapOf<String, Bound>()

    /**
     * Extracts a list of [DRI]s for checked exceptions from a [PsiMethod]'s throws clause.
     */
    public fun getCheckedExceptionDRIs(psiMethod: PsiMethod): List<DRI> =
        psiMethod.throwsList.toDriList()

    private fun PsiReferenceList.toDriList(): List<DRI> =
        referenceElements.mapNotNull { it?.resolve()?.let { resolved -> DRI.from(resolved) } }

    private fun <T> T.toSourceSetDependent() = mapOf(sourceSet to this)

    private fun <T : AnnotationTarget> PsiType.annotations(): PropertyContainer<T> =
        convertAnnotations(this.annotations.toList()).annotations()

    private fun <T : AnnotationTarget> List<Annotations.Annotation>.annotations(): PropertyContainer<T> =
        this.takeIf { it.isNotEmpty() }?.let { annotations ->
            PropertyContainer.withAll(annotations.toSourceSetDependent().toAnnotations())
        } ?: PropertyContainer.empty()

    // requires access to annotations, so extracted here
    public fun getBound(type: PsiType): Bound {
        //We would like to cache most of the bounds since it is not common to annotate them,
        //but if this is the case, we treat them as 'one of'
        fun PsiType.cacheBoundIfHasNoAnnotation(f: (List<Annotations.Annotation>) -> Bound): Bound {
            val annotations = convertAnnotations(this.annotations.toList())
            return if (annotations.isNotEmpty()) f(annotations)
            else cachedBounds.getOrPut(canonicalText) {
                f(annotations)
            }
        }

        return when (type) {
            is PsiClassType ->
                type.resolve()?.let { resolved ->
                    when {
                        resolved.qualifiedName == "java.lang.Object" -> type.cacheBoundIfHasNoAnnotation { annotations ->
                            JavaObject(
                                annotations.annotations()
                            )
                        }

                        resolved is PsiTypeParameter -> {
                            TypeParameter(
                                dri = DRI.from(resolved),
                                name = resolved.name.orEmpty(),
                                extra = type.annotations()
                            )
                        }

                        Regex("kotlin\\.jvm\\.functions\\.Function.*").matches(resolved.qualifiedName ?: "") ||
                                Regex("java\\.util\\.function\\.Function.*").matches(
                                    resolved.qualifiedName ?: ""
                                ) -> FunctionalTypeConstructor(
                            DRI.from(resolved),
                            type.parameters.map { getProjection(it) },
                            extra = type.annotations()
                        )

                        else -> {
                            // cache types that have no annotation and no type parameter
                            // since we cache only by name and type parameters depend on context
                            val typeParameters = type.parameters.map { getProjection(it) }
                            if (typeParameters.isEmpty())
                                type.cacheBoundIfHasNoAnnotation { annotations ->
                                    GenericTypeConstructor(
                                        DRI.from(resolved),
                                        typeParameters,
                                        extra = annotations.annotations()
                                    )
                                }
                            else
                                GenericTypeConstructor(
                                    DRI.from(resolved),
                                    typeParameters,
                                    extra = type.annotations()
                                )
                        }
                    }
                } ?: UnresolvedBound(type.presentableText, type.annotations())
            // aka vararg
            is PsiEllipsisType -> GenericTypeConstructor(
                DRI("kotlin", "Array"),
                listOf(Invariance(getBound(type.componentType))),
                extra = type.annotations()
            )
            is PsiArrayType -> GenericTypeConstructor(
                DRI("kotlin", "Array"),
                listOf(Invariance(getBound(type.componentType))), // kotlin.Array is invariant, but Java Array is covariant
                extra = type.annotations()
            )

            is PsiPrimitiveType -> if (type.name == "void") Void
            else type.cacheBoundIfHasNoAnnotation { annotations ->
                PrimitiveJavaType(
                    type.name,
                    annotations.annotations()
                )
            }

            else -> throw IllegalStateException("${type.presentableText} is not supported by PSI parser")
        }
    }

    private fun getVariance(type: PsiWildcardType): Projection = when {
        type.isExtends -> Covariance(getBound(type.extendsBound))
        type.isSuper -> Contravariance(getBound(type.superBound))
        // If the type isn't explicitly bounded, it still has an implicit `extends Object` bound
        type.extendsBound != PsiTypes.nullType() -> if ((type.extendsBound as PsiClassType).resolve()?.qualifiedName == "java.lang.Object") Star else Covariance(
            getBound(type.extendsBound)
        )
        else -> throw IllegalStateException("${type.presentableText} has incorrect bounds")
    }

    // requires access to annotations, so extracted here
    internal fun getProjection(type: PsiType): Projection = when (type) {
        is PsiEllipsisType -> Star
        is PsiWildcardType -> getVariance(type)
        else -> Invariance(getBound(type))
    }

    public fun convertAnnotations(annotations: Collection<PsiAnnotation>): List<Annotations.Annotation> =
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

    public fun convertAnnotation(annotation: PsiAnnotation): Annotations.Annotation? {
        // TODO Mitigating workaround for issue https://github.com/Kotlin/dokka/issues/1341
        //  Tracking https://youtrack.jetbrains.com/issue/KT-41234
        //  Needs to be removed once this issue is fixed in light classes
        fun PsiElement.getAnnotationsOrNull(): Array<PsiAnnotation>? {
            this as PsiClass
            return try {
                this.annotations
            } catch (e: Exception) {
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

    public fun getConstantExpression(field: PsiField): Expression? {
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
