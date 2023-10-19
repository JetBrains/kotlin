/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.commonizer.core

import org.jetbrains.kotlin.commonizer.cir.*
import org.jetbrains.kotlin.commonizer.utils.DEPRECATED_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.commonizer.utils.compactMap
import org.jetbrains.kotlin.commonizer.utils.compactMapOf

@Suppress("NOTHING_TO_INLINE")
object DeprecationAnnotationCommonizer : AssociativeCommonizer<CirAnnotation> {
    override fun commonize(first: CirAnnotation, second: CirAnnotation): CirAnnotation {
        val deprecationLevel = run {
            val firstLevel = first.getDeprecationLevel() ?: DeprecationLevel.WARNING
            val secondLevel = second.getDeprecationLevel() ?: DeprecationLevel.WARNING
            if (secondLevel.ordinal > firstLevel.ordinal) secondLevel else firstLevel
        }

        val deprecationMessage = run {
            val firstMessage = first.getDeprecationMessage() ?: return@run null
            val secondMessage = second.getDeprecationMessage() ?: return@run null
            if (firstMessage == secondMessage) firstMessage else null
        }

        val replaceWith = run {
            val firstReplaceWith = first.getReplaceWith() ?: return@run null
            val secondReplaceWith = second.getReplaceWith() ?: return@run null

            val firstReplaceWithExpression = firstReplaceWith.getReplaceWithExpression().orEmpty()
            val secondReplaceWithExpression = secondReplaceWith.getReplaceWithExpression().orEmpty()

            val firstReplaceWithImports = firstReplaceWith.getReplaceWithImports().orEmpty()
            val secondReplaceWithImports = secondReplaceWith.getReplaceWithImports().orEmpty()

            if (
                firstReplaceWithExpression == secondReplaceWithExpression &&
                firstReplaceWithImports == secondReplaceWithImports &&
                /* Empty replace with */
                (firstReplaceWithExpression.isNotEmpty() || firstReplaceWithImports.isNotEmpty())
            ) firstReplaceWithExpression.toReplaceWithValue(firstReplaceWithImports) else null
        }


        val constantValueArguments: Map<CirName, CirConstantValue> = if (deprecationLevel == DeprecationLevel.WARNING) {
            // don't populate with the default level value
            compactMapOf(PROPERTY_NAME_MESSAGE, deprecationMessage.toDeprecationMessageValue())
        } else compactMapOf(
            PROPERTY_NAME_MESSAGE, deprecationMessage.toDeprecationMessageValue(),
            PROPERTY_NAME_LEVEL, deprecationLevel.toDeprecationLevelValue()
        )

        val annotationValueArguments: Map<CirName, CirAnnotation> = if (replaceWith == null) {
            // don't populate with empty (default) ReplaceWith
            emptyMap()
        } else compactMapOf(PROPERTY_NAME_REPLACE_WITH, replaceWith)

        return CirAnnotation.createInterned(
            type = DEPRECATED_ANNOTATION_TYPE,
            constantValueArguments = constantValueArguments,
            annotationValueArguments = annotationValueArguments
        )
    }

    private val PROPERTY_NAME_MESSAGE = CirName.create(Deprecated::message.name)
    private val PROPERTY_NAME_REPLACE_WITH = CirName.create(Deprecated::replaceWith.name)
    private val PROPERTY_NAME_LEVEL = CirName.create(Deprecated::level.name)

    private val PROPERTY_NAME_EXPRESSION = CirName.create(ReplaceWith::expression.name)
    private val PROPERTY_NAME_IMPORTS = CirName.create(ReplaceWith::imports.name)

    internal const val FALLBACK_MESSAGE = "See concrete deprecation messages in actual declarations"


    // Optimization: Keep most frequently used message constants.
    private val FREQUENTLY_USED_MESSAGE_VALUES: Map<String, CirConstantValue.StringValue> = listOf(
        "Use constructor instead",
        "Use factory method instead"
    ).associateWith { CirConstantValue.StringValue(it) }
    private val FALLBACK_MESSAGE_VALUE = CirConstantValue.StringValue(FALLBACK_MESSAGE)

    private val DEPRECATED_ANNOTATION_TYPE = buildAnnotationType(DEPRECATED_ANNOTATION_CLASS_ID)
    private val REPLACE_WITH_ANNOTATION_TYPE = buildAnnotationType(CirEntityId.create("kotlin/ReplaceWith"))

    private val DEPRECATION_LEVEL_CLASS_ID = CirEntityId.create("kotlin/DeprecationLevel")

    // Optimization: Keep DeprecationLevel enum constants.
    // TODO: replace with `entries` when KT-62702 will be fixed
    private val DEPRECATION_LEVEL_ENUM_ENTRY_VALUES: Map<String, CirConstantValue.EnumValue> = DeprecationLevel.values().associate {
        it.name to CirConstantValue.EnumValue(DEPRECATION_LEVEL_CLASS_ID, CirName.create(it.name))
    }

    private fun buildAnnotationType(classId: CirEntityId) = CirClassType.createInterned(
        classId = classId,
        outerType = null,
        arguments = emptyList(),
        isMarkedNullable = false
    )

    private fun CirAnnotation.getDeprecationMessage(): String? = constantValueArguments.getString(PROPERTY_NAME_MESSAGE)

    private fun String?.toDeprecationMessageValue(): CirConstantValue.StringValue =
        if (this == null)
            FALLBACK_MESSAGE_VALUE
        else
            FREQUENTLY_USED_MESSAGE_VALUES[this] ?: CirConstantValue.StringValue(this)

    private fun CirAnnotation.getDeprecationLevel(): DeprecationLevel? {
        val enumEntryName = constantValueArguments.getEnumEntryName(PROPERTY_NAME_LEVEL) ?: return null
        // TODO: replace with `entries` when KT-62702 will be fixed
        return DeprecationLevel.values().firstOrNull { it.name == enumEntryName }
    }

    private fun DeprecationLevel.toDeprecationLevelValue(): CirConstantValue.EnumValue =
        DEPRECATION_LEVEL_ENUM_ENTRY_VALUES.getValue(name)

    private fun CirAnnotation.getReplaceWith(): CirAnnotation? =
        annotationValueArguments.getAnnotation(PROPERTY_NAME_REPLACE_WITH)

    private fun CirAnnotation.getReplaceWithExpression(): String? =
        constantValueArguments.getString(PROPERTY_NAME_EXPRESSION)

    private fun CirAnnotation.getReplaceWithImports(): List<String>? =
        constantValueArguments.getStringArray(PROPERTY_NAME_IMPORTS)

    private fun String.toReplaceWithValue(imports: List<String>): CirAnnotation =
        createReplaceWithAnnotation(this, imports)

    private inline fun Map<CirName, CirConstantValue>.getString(name: CirName): String? =
        (this[name] as? CirConstantValue.StringValue)?.value

    private inline fun Map<CirName, CirConstantValue>.getEnumEntryName(name: CirName): String? =
        (this[name] as? CirConstantValue.EnumValue)?.enumEntryName?.name

    private inline fun Map<CirName, CirAnnotation>.getAnnotation(name: CirName): CirAnnotation? =
        this[name]

    private inline fun Map<CirName, CirConstantValue>.getStringArray(name: CirName): List<String>? {
        val elements: List<CirConstantValue> = (this[name] as? CirConstantValue.ArrayValue)?.elements ?: return null
        if (elements.isEmpty()) return emptyList()

        val result = ArrayList<String>(elements.size)
        for (element in elements) {
            if (element is CirConstantValue.StringValue) {
                result += element.value
            } else
                return null
        }

        return result
    }

    private inline fun createReplaceWithAnnotation(expression: String, imports: List<String>): CirAnnotation =
        CirAnnotation.createInterned(
            type = REPLACE_WITH_ANNOTATION_TYPE,
            constantValueArguments = compactMapOf(
                PROPERTY_NAME_EXPRESSION, CirConstantValue.StringValue(expression),
                PROPERTY_NAME_IMPORTS, CirConstantValue.ArrayValue(imports.compactMap(CirConstantValue::StringValue))
            ),
            annotationValueArguments = emptyMap()
        )
}
