/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirAnnotationFactory
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirTypeFactory
import org.jetbrains.kotlin.descriptors.commonizer.core.AnnotationsCommonizer.Companion.FALLBACK_MESSAGE
import org.jetbrains.kotlin.descriptors.commonizer.utils.DEPRECATED_ANNOTATION_CLASS_ID
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMap
import org.jetbrains.kotlin.descriptors.commonizer.utils.compactMapOf
import org.jetbrains.kotlin.descriptors.commonizer.utils.intern
import org.jetbrains.kotlin.descriptors.commonizer.utils.internedClassId
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import kotlin.DeprecationLevel.WARNING

/**
 * This is limited implementation of annotations commonizer. It helps to commonize only [kotlin.Deprecated] annotations.
 */
class AnnotationsCommonizer : AbstractStandardCommonizer<List<CirAnnotation>, List<CirAnnotation>>() {
    private var deprecatedAnnotationCommonizer: DeprecatedAnnotationCommonizer? = null

    override fun commonizationResult(): List<CirAnnotation> {
        val deprecatedAnnotation = deprecatedAnnotationCommonizer?.result ?: return emptyList()
        return listOf(deprecatedAnnotation)
    }

    override fun initialize(first: List<CirAnnotation>) = Unit

    override fun doCommonizeWith(next: List<CirAnnotation>): Boolean {
        val nextDeprecatedAnnotation = next.firstOrNull { it.type.classifierId == DEPRECATED_ANNOTATION_CLASS_ID } ?: return true

        val deprecatedAnnotationCommonizer = deprecatedAnnotationCommonizer
            ?: DeprecatedAnnotationCommonizer().also { this.deprecatedAnnotationCommonizer = it }

        return deprecatedAnnotationCommonizer.commonizeWith(nextDeprecatedAnnotation)
    }

    companion object {
        internal const val FALLBACK_MESSAGE = "See concrete deprecation messages in actual declarations"
    }
}

private class DeprecatedAnnotationCommonizer : Commonizer<CirAnnotation, CirAnnotation> {
    private var level: DeprecationLevel? = null // null level means that state is empty
    private var message: String? = null // null -> message is not equal
    private lateinit var replaceWithExpression: String
    private lateinit var replaceWithImports: List<String>

    override val result: CirAnnotation
        get() {
            val level: DeprecationLevel = level ?: failInEmptyState()
            val messageValue: StringValue = message.toDeprecationMessageValue()

            val constantValueArguments: Map<Name, ConstantValue<*>> = if (level == WARNING) {
                // don't populate with the default level value
                compactMapOf(PROPERTY_NAME_MESSAGE, messageValue)
            } else
                compactMapOf(
                    PROPERTY_NAME_MESSAGE, messageValue,
                    PROPERTY_NAME_LEVEL, level.toDeprecationLevelValue()
                )

            val annotationValueArguments: Map<Name, CirAnnotation> = if (replaceWithExpression.isEmpty() && replaceWithImports.isEmpty()) {
                // don't populate with empty (default) ReplaceWith
                emptyMap()
            } else
                compactMapOf(PROPERTY_NAME_REPLACE_WITH, replaceWithExpression.toReplaceWithValue(replaceWithImports))

            return CirAnnotationFactory.create(
                type = DEPRECATED_ANNOTATION_TYPE,
                constantValueArguments = constantValueArguments,
                annotationValueArguments = annotationValueArguments
            )
        }

    override fun commonizeWith(next: CirAnnotation): Boolean {
        val nextLevel: DeprecationLevel = next.getDeprecationLevel() ?: WARNING
        val nextMessage: String = next.getDeprecationMessage().orEmpty()
        val nextReplaceWith: CirAnnotation? = next.getReplaceWith()
        val nextReplaceWithExpression: String = nextReplaceWith?.getReplaceWithExpression().orEmpty()
        val nextReplaceWithImports: List<String> = nextReplaceWith?.getReplaceWithImports().orEmpty()

        return if (level != null) {
            doCommonizeWith(nextLevel, nextMessage, nextReplaceWithExpression, nextReplaceWithImports)
        } else {
            // empty, just fill in
            initialize(nextLevel, nextMessage, nextReplaceWithExpression, nextReplaceWithImports)
            true
        }
    }

    private fun initialize(
        nextLevel: DeprecationLevel,
        nextMessage: String?,
        nextReplaceWithExpression: String,
        nextReplaceWithImports: List<String>
    ) {
        level = nextLevel
        message = nextMessage
        replaceWithExpression = nextReplaceWithExpression
        replaceWithImports = nextReplaceWithImports
    }

    private fun doCommonizeWith(
        nextLevel: DeprecationLevel,
        nextMessage: String?,
        nextReplaceWithExpression: String,
        nextReplaceWithImports: List<String>
    ): Boolean {
        if (nextLevel.ordinal > level!!.ordinal)
            level = nextLevel

        if (nextMessage != message)
            message = null

        if (nextReplaceWithExpression != replaceWithExpression || nextReplaceWithImports != replaceWithImports) {
            replaceWithExpression = ""
            replaceWithImports = emptyList()
        }

        return true
    }

    @Suppress("NOTHING_TO_INLINE")
    companion object {
        private val PROPERTY_NAME_MESSAGE = Name.identifier(Deprecated::message.name).intern()
        private val PROPERTY_NAME_REPLACE_WITH = Name.identifier(Deprecated::replaceWith.name).intern()
        private val PROPERTY_NAME_LEVEL = Name.identifier(Deprecated::level.name).intern()

        private val PROPERTY_NAME_EXPRESSION = Name.identifier(ReplaceWith::expression.name).intern()
        private val PROPERTY_NAME_IMPORTS = Name.identifier(ReplaceWith::imports.name).intern()

        // Optimization: Keep most frequently used message constants.
        private val FREQUENTLY_USED_MESSAGE_VALUES: Map<String, StringValue> = listOf(
            "Use constructor instead",
            "Use factory method instead"
        ).associateWith { StringValue(it) }
        private val FALLBACK_MESSAGE_VALUE = StringValue(FALLBACK_MESSAGE)

        private val DEPRECATED_ANNOTATION_TYPE = buildAnnotationType(DEPRECATED_ANNOTATION_CLASS_ID)
        private val REPLACE_WITH_ANNOTATION_TYPE = buildAnnotationType(internedClassId(FqName(ReplaceWith::class.java.name)))

        private val DEPRECATION_LEVEL_CLASS_ID = internedClassId(FqName(DeprecationLevel::class.java.name))

        // Optimization: Keep DeprecationLevel enum constants.
        private val DEPRECATION_LEVEL_ENUM_ENTRY_VALUES: Map<String, EnumValue> = DeprecationLevel.values().associate {
            it.name to EnumValue(DEPRECATION_LEVEL_CLASS_ID, Name.identifier(it.name).intern())
        }

        private fun buildAnnotationType(classId: ClassId) = CirTypeFactory.createClassType(
            classId = classId,
            outerType = null,
            visibility = DescriptorVisibilities.PUBLIC,
            arguments = emptyList(),
            isMarkedNullable = false
        )

        private fun CirAnnotation.getDeprecationMessage(): String? = constantValueArguments.getString(PROPERTY_NAME_MESSAGE)

        private fun String?.toDeprecationMessageValue(): StringValue =
            if (this == null)
                FALLBACK_MESSAGE_VALUE
            else
                FREQUENTLY_USED_MESSAGE_VALUES[this] ?: StringValue(this)

        private fun CirAnnotation.getDeprecationLevel(): DeprecationLevel? {
            val enumEntryName = constantValueArguments.getEnumEntryName(PROPERTY_NAME_LEVEL) ?: return null
            return DeprecationLevel.values().firstOrNull { it.name == enumEntryName }
        }

        private fun DeprecationLevel.toDeprecationLevelValue(): EnumValue =
            DEPRECATION_LEVEL_ENUM_ENTRY_VALUES.getValue(name)

        private fun CirAnnotation.getReplaceWith(): CirAnnotation? =
            annotationValueArguments.getAnnotation(PROPERTY_NAME_REPLACE_WITH)

        private fun CirAnnotation.getReplaceWithExpression(): String? =
            constantValueArguments.getString(PROPERTY_NAME_EXPRESSION)

        private fun CirAnnotation.getReplaceWithImports(): List<String>? =
            constantValueArguments.getStringArray(PROPERTY_NAME_IMPORTS)

        private fun String.toReplaceWithValue(imports: List<String>): CirAnnotation =
            createReplaceWithAnnotation(this, imports)

        private inline fun Map<Name, ConstantValue<*>>.getString(name: Name): String? =
            (this[name] as? StringValue)?.value

        private inline fun Map<Name, ConstantValue<*>>.getEnumEntryName(name: Name): String? =
            (this[name] as? EnumValue)?.enumEntryName?.asString()

        private inline fun Map<Name, CirAnnotation>.getAnnotation(name: Name): CirAnnotation? =
            this[name]

        private inline fun Map<Name, ConstantValue<*>>.getStringArray(name: Name): List<String>? {
            val elements: List<ConstantValue<*>> = (this[name] as? ArrayValue)?.value ?: return null
            if (elements.isEmpty()) return emptyList()

            val result = ArrayList<String>(elements.size)
            for (element in elements) {
                if (element is StringValue) {
                    result += element.value
                } else
                    return null
            }

            return result
        }

        private inline fun createReplaceWithAnnotation(expression: String, imports: List<String>): CirAnnotation =
            CirAnnotationFactory.create(
                type = REPLACE_WITH_ANNOTATION_TYPE,
                constantValueArguments = compactMapOf(
                    PROPERTY_NAME_EXPRESSION, StringValue(expression),
                    PROPERTY_NAME_IMPORTS, ArrayValue(
                        value = imports.compactMap { StringValue(it) },
                        computeType = { it.builtIns.getArrayElementType(it.builtIns.stringType) }
                    )
                ),
                annotationValueArguments = emptyMap()
            )
    }
}
