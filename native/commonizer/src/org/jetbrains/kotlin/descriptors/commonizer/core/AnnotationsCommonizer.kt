/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.descriptors.commonizer.core

import org.jetbrains.kotlin.descriptors.commonizer.cir.CirAnnotation
import org.jetbrains.kotlin.descriptors.commonizer.cir.factory.CirAnnotationFactory
import org.jetbrains.kotlin.descriptors.commonizer.core.AnnotationsCommonizer.Companion.FALLBACK_MESSAGE
import org.jetbrains.kotlin.descriptors.commonizer.utils.DEPRECATED_ANNOTATION_FQN
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
        val nextDeprecatedAnnotation = next.firstOrNull { it.fqName == DEPRECATED_ANNOTATION_FQN } ?: return true
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
            val level: DeprecationLevel = level ?: throw IllegalCommonizerStateException()
            val messageValue: StringValue = message.toDeprecationMessageValue()

            val constantValueArguments: Map<Name, ConstantValue<*>> = if (level == WARNING) {
                // don't populate with the default level value
                mapOf(PROPERTY_NAME_MESSAGE to messageValue)
            } else
                hashMapOf(
                    PROPERTY_NAME_MESSAGE to messageValue,
                    PROPERTY_NAME_LEVEL to level.toDeprecationLevelValue()
                )

            val annotationValueArguments: Map<Name, CirAnnotation> = if (replaceWithExpression.isEmpty() && replaceWithImports.isEmpty()) {
                // don't populate with empty (default) ReplaceWith
                emptyMap()
            } else
                mapOf(PROPERTY_NAME_REPLACE_WITH to replaceWithExpression.toReplaceWithValue(replaceWithImports))

            return CirAnnotationFactory.create(
                fqName = DEPRECATED_ANNOTATION_FQN,
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
        private val PROPERTY_NAME_MESSAGE = Name.identifier(Deprecated::message.name)
        private val PROPERTY_NAME_REPLACE_WITH = Name.identifier(Deprecated::replaceWith.name)
        private val PROPERTY_NAME_LEVEL = Name.identifier(Deprecated::level.name)

        private val PROPERTY_NAME_EXPRESSION = Name.identifier(ReplaceWith::expression.name)
        private val PROPERTY_NAME_IMPORTS = Name.identifier(ReplaceWith::imports.name)

        // Optimization: Keep most frequently used message constants.
        private val FREQUENTLY_USED_MESSAGE_VALUES: Map<String, StringValue> = listOf(
            "Use constructor instead",
            "Use factory method instead"
        ).associateWith { StringValue(it) }
        private val FALLBACK_MESSAGE_VALUE = StringValue(FALLBACK_MESSAGE)

        private val DEPRECATION_LEVEL_FQN = FqName(DeprecationLevel::class.java.name)
        private val DEPRECATION_LEVEL_CLASS_ID = ClassId.topLevel(DEPRECATION_LEVEL_FQN)

        // Optimization: Keep DeprecationLevel enum constants.
        private val DEPRECATION_LEVEL_ENUM_ENTRY_VALUES: Map<String, EnumValue> = DeprecationLevel.values().associate {
            it.name to EnumValue(DEPRECATION_LEVEL_CLASS_ID, Name.identifier(it.name))
        }

        private val REPLACE_WITH_FQN = FqName(ReplaceWith::class.java.name)

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

            val result = mutableListOf<String>()
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
                fqName = REPLACE_WITH_FQN,
                constantValueArguments = mapOf(
                    PROPERTY_NAME_EXPRESSION to StringValue(expression),
                    PROPERTY_NAME_IMPORTS to ArrayValue(
                        value = imports.map { StringValue(it) },
                        computeType = { it.builtIns.getArrayElementType(it.builtIns.stringType) }
                    )
                ),
                annotationValueArguments = emptyMap()
            )
    }
}
