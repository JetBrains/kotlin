/*
 * Copyright 2014-2024 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license.
 */

package org.jetbrains.dokka.analysis.kotlin.symbols.translators

import org.jetbrains.dokka.links.DRI
import org.jetbrains.dokka.links.withEnumEntryExtra
import org.jetbrains.dokka.model.*
import org.jetbrains.dokka.model.ClassValue
import org.jetbrains.kotlin.analysis.api.KtAnalysisSession
import org.jetbrains.kotlin.analysis.api.annotations.*
import org.jetbrains.kotlin.analysis.api.base.KtConstantValue
import org.jetbrains.kotlin.analysis.api.symbols.KtPropertySymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbol
import org.jetbrains.kotlin.analysis.api.symbols.KtSymbolOrigin
import org.jetbrains.kotlin.descriptors.annotations.AnnotationUseSiteTarget
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile

/**
 * Map [KtAnnotationApplication] to Dokka [Annotations.Annotation]
 */
internal class AnnotationTranslator {
    private fun KtAnalysisSession.getFileLevelAnnotationsFrom(symbol: KtSymbol) =
        if (symbol.origin != KtSymbolOrigin.SOURCE)
            null
        else
            (symbol.psi?.containingFile as? KtFile)?.getFileSymbol()?.annotations
                ?.map { toDokkaAnnotation(it) }

    private fun KtAnalysisSession.getDirectAnnotationsFrom(annotated: KtAnnotated) =
        annotated.annotations.map { toDokkaAnnotation(it) }

    /**
     * The examples of annotations from backing field are [JvmField], [JvmSynthetic].
     *
     * @return direct annotations, annotations from backing field and file-level annotations
     */
    fun KtAnalysisSession.getAllAnnotationsFrom(annotated: KtAnnotated): List<Annotations.Annotation> {
        val directAnnotations = getDirectAnnotationsFrom(annotated)
        val backingFieldAnnotations =
            (annotated as? KtPropertySymbol)?.backingFieldSymbol?.let { getDirectAnnotationsFrom(it) }.orEmpty()
        val fileLevelAnnotations = (annotated as? KtSymbol)?.let { getFileLevelAnnotationsFrom(it) }.orEmpty()
        return directAnnotations + backingFieldAnnotations + fileLevelAnnotations
    }

    private fun KtAnnotationApplication.isNoExistedInSource() = psi == null
    private fun AnnotationUseSiteTarget.toDokkaAnnotationScope(): Annotations.AnnotationScope = when (this) {
        AnnotationUseSiteTarget.PROPERTY_GETTER -> Annotations.AnnotationScope.DIRECT // due to compatibility with Dokka K1
        AnnotationUseSiteTarget.PROPERTY_SETTER -> Annotations.AnnotationScope.DIRECT // due to compatibility with Dokka K1
        AnnotationUseSiteTarget.FILE -> Annotations.AnnotationScope.FILE
        else -> Annotations.AnnotationScope.DIRECT
    }

    private fun KtAnalysisSession.mustBeDocumented(annotationApplication: KtAnnotationApplication): Boolean {
        if (annotationApplication.isNoExistedInSource()) return false
        val annotationClass = getClassOrObjectSymbolByClassId(annotationApplication.classId ?: return false)
        return annotationClass?.hasAnnotation(mustBeDocumentedAnnotation)
            ?: false
    }

    private fun KtAnalysisSession.toDokkaAnnotation(annotationApplication: KtAnnotationApplication) =
        Annotations.Annotation(
            dri = annotationApplication.classId?.createDRI()
                ?: DRI(packageName = "", classNames = ERROR_CLASS_NAME), // classId might be null on a non-existing annotation call,
            params = if (annotationApplication is KtAnnotationApplicationWithArgumentsInfo) annotationApplication.arguments.associate {
                it.name.asString() to toDokkaAnnotationValue(
                    it.expression
                )
            } else emptyMap(),
            mustBeDocumented = mustBeDocumented(annotationApplication),
            scope = annotationApplication.useSiteTarget?.toDokkaAnnotationScope() ?: Annotations.AnnotationScope.DIRECT
        )

    @OptIn(ExperimentalUnsignedTypes::class)
    private fun KtAnalysisSession.toDokkaAnnotationValue(annotationValue: KtAnnotationValue): AnnotationParameterValue =
        when (annotationValue) {
            is KtConstantAnnotationValue -> {
                when (val value = annotationValue.constantValue) {
                    is KtConstantValue.KtNullConstantValue -> NullValue
                    is KtConstantValue.KtFloatConstantValue -> FloatValue(value.value)
                    is KtConstantValue.KtDoubleConstantValue -> DoubleValue(value.value)
                    is KtConstantValue.KtLongConstantValue -> LongValue(value.value)
                    is KtConstantValue.KtIntConstantValue -> IntValue(value.value)
                    is KtConstantValue.KtBooleanConstantValue -> BooleanValue(value.value)
                    is KtConstantValue.KtByteConstantValue -> IntValue(value.value.toInt())
                    is KtConstantValue.KtCharConstantValue -> StringValue(value.value.toString())
                    is KtConstantValue.KtErrorConstantValue -> StringValue(value.renderAsKotlinConstant())
                    is KtConstantValue.KtShortConstantValue -> IntValue(value.value.toInt())
                    is KtConstantValue.KtStringConstantValue -> StringValue(value.value)
                    is KtConstantValue.KtUnsignedByteConstantValue -> IntValue(value.value.toInt())
                    is KtConstantValue.KtUnsignedIntConstantValue -> IntValue(value.value.toInt())
                    is KtConstantValue.KtUnsignedLongConstantValue -> LongValue(value.value.toLong())
                    is KtConstantValue.KtUnsignedShortConstantValue -> IntValue(value.value.toInt())
                }
            }

            is KtEnumEntryAnnotationValue -> EnumValue(
                with(annotationValue.callableId) { this?.className?.asString() + "." + this?.callableName?.asString() },
                getDRIFrom(annotationValue)
            )

            is KtArrayAnnotationValue -> ArrayValue(annotationValue.values.map { toDokkaAnnotationValue(it) })
            is KtAnnotationApplicationValue -> AnnotationValue(toDokkaAnnotation(annotationValue.annotationValue))
            is KtKClassAnnotationValue.KtNonLocalKClassAnnotationValue -> ClassValue(
                annotationValue.classId.relativeClassName.asString(),
                annotationValue.classId.createDRI()
            )

            is KtKClassAnnotationValue.KtLocalKClassAnnotationValue -> throw IllegalStateException("Unexpected a local class in annotation")
            is KtKClassAnnotationValue.KtErrorClassAnnotationValue -> ClassValue(
                annotationValue.unresolvedQualifierName ?: "",
                DRI(packageName = "", classNames = ERROR_CLASS_NAME)
            )

            KtUnsupportedAnnotationValue -> ClassValue(
                "<Unsupported Annotation Value>",
                DRI(packageName = "", classNames = ERROR_CLASS_NAME)
            )
        }

    private fun getDRIFrom(enumEntry: KtEnumEntryAnnotationValue): DRI {
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
        internal fun KtAnnotated.getPresentableName(): String? =
            this.annotationsByClassId(parameterNameAnnotation)
                .firstOrNull()?.arguments?.firstOrNull { it.name == Name.identifier("name") }?.expression?.let { it as? KtConstantAnnotationValue }
                ?.let { it.constantValue.value.toString() }
    }
}
