/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.translators

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withEnumEntryExtra
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.ClassValue
import org.jetbrains.kotlin.analysis.api.KaSession
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.base.KaConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.KaPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KaSymbolOrigin
import org.jetbrains.kotlin.analysis.api.types.*
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

/**
 * Map [KtAnnotationApplication] to Dokka [Annotations.Annotation]
 */
internal class AnnotationTranslator {
    private fun KaSession.getFileLevelAnnotationsFrom(symbol: KaSymbol) =
        if (symbol.origin != KaSymbolOrigin.SOURCE)
            null
        else
            (symbol.psi?.containingFile as? KtFile)?.getFileSymbol()?.annotations
                ?.map { toDokkaAnnotation(it) }

    private fun KaSession.getDirectAnnotationsFrom(annotated: KaAnnotated) =
        annotated.annotations.map { toDokkaAnnotation(it) }

    /**
     * The examples of annotations from backing field are [JvmField], [JvmSynthetic].
     *
     * @return direct annotations, annotations from backing field and file-level annotations
     */
    fun KaSession.getAllAnnotationsFrom(annotated: KaAnnotated): List<Annotations.Annotation> {
        val directAnnotations = getDirectAnnotationsFrom(annotated)
        val backingFieldAnnotations =
            (annotated as? KaPropertySymbol)?.backingFieldSymbol?.let { getDirectAnnotationsFrom(it) }.orEmpty()
        val fileLevelAnnotations = (annotated as? KaSymbol)?.let { getFileLevelAnnotationsFrom(it) }.orEmpty()
        return directAnnotations + backingFieldAnnotations + fileLevelAnnotations
    }

    private fun KaAnnotationApplication.isNoExistedInSource() = psi == null
    private fun AnnotationUseSiteTarget.toDokkaAnnotationScope(): Annotations.AnnotationScope = when (this) {
        AnnotationUseSiteTarget.PROPERTY_GETTER -> Annotations.AnnotationScope.DIRECT // due to compatibility with Dokka K1
        AnnotationUseSiteTarget.PROPERTY_SETTER -> Annotations.AnnotationScope.DIRECT // due to compatibility with Dokka K1
        AnnotationUseSiteTarget.FILE -> Annotations.AnnotationScope.FILE
        else -> Annotations.AnnotationScope.DIRECT
    }

    private fun KaSession.mustBeDocumented(annotationApplication: KaAnnotationApplication): Boolean {
        if (annotationApplication.isNoExistedInSource()) return false
        val annotationClass = getClassOrObjectSymbolByClassId(annotationApplication.classId ?: return false)
        return annotationClass?.let { mustBeDocumentedAnnotation in it.annotations }
            ?: false
    }

    private fun KaSession.toDokkaAnnotation(annotationApplication: KaAnnotationApplication) =
        Annotations.Annotation(
            dri = annotationApplication.classId?.createDRI()
                ?: DRI(packageName = "", classNames = ERROR_CLASS_NAME), // classId might be null on a non-existing annotation call,
            params = annotationApplication.arguments.associate {
                it.name.asString() to toDokkaAnnotationValue(
                    it.expression
                )
            },
            mustBeDocumented = mustBeDocumented(annotationApplication),
            scope = annotationApplication.useSiteTarget?.toDokkaAnnotationScope() ?: Annotations.AnnotationScope.DIRECT
        )

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun KaSession.toDokkaAnnotationValue(annotationValue: KaAnnotationValue): AnnotationParameterValue =
        when (annotationValue) {
            is KaConstantAnnotationValue -> {
                when (val value = annotationValue.constantValue) {
                    is KaConstantValue.KaNullConstantValue -> NullValue
                    is KaConstantValue.KaFloatConstantValue -> FloatValue(value.value)
                    is KaConstantValue.KaDoubleConstantValue -> DoubleValue(value.value)
                    is KaConstantValue.KaLongConstantValue -> LongValue(value.value)
                    is KaConstantValue.KaIntConstantValue -> IntValue(value.value)
                    is KaConstantValue.KaBooleanConstantValue -> BooleanValue(value.value)
                    is KaConstantValue.KaByteConstantValue -> IntValue(value.value.toInt())
                    is KaConstantValue.KaCharConstantValue -> StringValue(value.value.toString())
                    is KaConstantValue.KaErrorConstantValue -> StringValue(value.renderAsKotlinConstant())
                    is KaConstantValue.KaShortConstantValue -> IntValue(value.value.toInt())
                    is KaConstantValue.KaStringConstantValue -> StringValue(value.value)
                    is KaConstantValue.KaUnsignedByteConstantValue -> IntValue(value.value.toInt())
                    is KaConstantValue.KaUnsignedIntConstantValue -> IntValue(value.value.toInt())
                    is KaConstantValue.KaUnsignedLongConstantValue -> LongValue(value.value.toLong())
                    is KaConstantValue.KaUnsignedShortConstantValue -> IntValue(value.value.toInt())
                }
            }

            is KaEnumEntryAnnotationValue -> EnumValue(
                with(annotationValue.callableId) { this?.className?.asString() + "." + this?.callableName?.asString() },
                getDRIFrom(annotationValue)
            )

            is KaArrayAnnotationValue -> ArrayValue(annotationValue.values.map { toDokkaAnnotationValue(it) })
            is KaAnnotationApplicationValue -> AnnotationValue(toDokkaAnnotation(annotationValue.annotationValue))
            is KaKClassAnnotationValue -> when (val type: KaType = annotationValue.type) {
                is KaNonErrorClassType -> ClassValue(
                    type.classId.relativeClassName.asString(),
                    type.classId.createDRI()
                )
                else -> ClassValue(
                    type.toString(),
                    DRI(packageName = "", classNames = ERROR_CLASS_NAME)
                )
            }
            is KaUnsupportedAnnotationValue -> ClassValue(
                "<Unsupported Annotation Value>",
                DRI(packageName = "", classNames = ERROR_CLASS_NAME)
            )
        }

    private fun getDRIFrom(enumEntry: KaEnumEntryAnnotationValue): DRI {
        val callableId =
            enumEntry.callableId ?: throw IllegalStateException("Can't get `callableId` for enum entry from annotation")
        return DRI(
            packageName = callableId.packageName.asString(),
            classNames = callableId.className?.asString() + "." + callableId.callableName.asString(),
        ).withEnumEntryExtra()
    }

    companion object {
        val mustBeDocumentedAnnotation = ClassId(FqName("kotlin.annotation"), FqName("MustBeDocumented"), false)
        private val parameterNameAnnotation = ClassId(FqName("kotlin"), FqName("ParameterName"), false)

        /**
         * Functional types can have **generated** [ParameterName] annotation
         * @see ParameterName
         */
        internal fun KaAnnotated.getPresentableName(): String? =
            this.annotations[parameterNameAnnotation]
                .firstOrNull()?.arguments?.firstOrNull { it.name == Name.identifier("name") }?.expression?.let { it as? KaConstantAnnotationValue }
                ?.let { it.constantValue.value.toString() }
    }
}
