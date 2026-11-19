/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.documentation.*
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonArray

/**
 * Golden tests printing selected parts of every [KdElement] in every fragment, including embedded declarations.
 * Each entry is printed as `signature [element ID]<path>:` followed by a JSON dump of the value.
 * Expected output is stored beside the fixture, with variants where Java analysis modes differ.
 */
abstract class AbstractElementDumpGoldenTest : AbstractKdmGoldenTest() {
    protected abstract val extension: String

    /** Returns the values to print for [element], keyed by their path relative to the element, in print order. */
    protected abstract fun selectValues(element: KdElement): Map<String, Any?>

    fun runTest(testDataFilePath: String) {
        val file = ForTestCompileRuntime.transformTestDataPath(testDataFilePath)
        val bundle = generateDocumentationBundleFromTestData(file)
        val actual = buildString {
            for (fragment in bundle.fragments) {
                appendLine("fragment ${fragment.name}")
                for ((mapName, elements) in listOf("elements" to fragment.elements, "embeddedElements" to fragment.embeddedElements)) {
                    for ((id, element) in elements) {
                        val values = selectValues(element)
                        if (values.isEmpty()) continue
                        val signature = element.compactSignature()
                        val prefix = if (mapName == "embeddedElements") "$mapName: " else ""
                        for ((path, value) in values) {
                            appendLine("$prefix$signature [${id.value}]$path:")
                            appendLine(encodeValue(value))
                        }
                        appendLine()
                    }
                }
            }
        }
        assertMatchesGoldenTextFile(actual, file, extension)
    }
}

/**
 * Encodes [value] as JSON. Model types are encoded via their own serializers; [KdType]s are encoded polymorphically,
 * so their `kind` is visible. Lists are built element-by-element into a [JsonArray].
 */
private fun encodeValue(value: Any?): String =
    json.encodeToString(JsonElement.serializer(), jsonElement(value))

private fun jsonElement(value: Any?): JsonElement = when (value) {
    null -> JsonNull
    is String -> JsonPrimitive(value)
    is KdType -> json.encodeToJsonElement(KdType.serializer(), value)
    is KdAnnotation -> json.encodeToJsonElement(KdAnnotation.serializer(), value)
    is KdDeprecated -> json.encodeToJsonElement(KdDeprecated.serializer(), value)
    is KdThrows -> json.encodeToJsonElement(KdThrows.serializer(), value)
    is KdElementId -> json.encodeToJsonElement(KdElementId.serializer(), value)
    is List<*> -> JsonArray(value.map(::jsonElement))
    else -> error("Unsupported golden-dump value: $value")
}

/** Prints one [KdElement] field; absent fields, null values, and empty lists are omitted. */
abstract class AbstractElementFieldGoldenTest(
    private val fieldName: String,
    private val field: (KdElement) -> Any?,
) : AbstractElementDumpGoldenTest() {
    override val extension: String get() = ".txt"

    override fun selectValues(element: KdElement): Map<String, Any?> {
        val value = field(element) ?: return emptyMap()
        if (value is List<*> && value.isEmpty()) return emptyMap()
        return mapOf(".$fieldName" to value)
    }
}

abstract class AbstractAnnotationsGoldenTest : AbstractElementFieldGoldenTest("annotations", { element ->
    when (element) {
        is KdModule -> element.annotations
        is KdPackage -> element.annotations
        is KdClass -> element.annotations
        is KdFunction -> element.annotations
        is KdProperty -> element.annotations
        is KdConstructor -> element.annotations
        is KdTypealias -> element.annotations
        is KdRefinedElement, is KdInheritedMember -> null
    }
})

abstract class AbstractDeprecatedGoldenTest : AbstractElementFieldGoldenTest("deprecated", { element ->
    when (element) {
        is KdClass -> element.deprecated
        is KdFunction -> element.deprecated
        is KdProperty -> element.deprecated
        is KdConstructor -> element.deprecated
        is KdTypealias -> element.deprecated
        is KdModule, is KdPackage, is KdRefinedElement, is KdInheritedMember -> null
    }
})

abstract class AbstractSinceGoldenTest : AbstractElementFieldGoldenTest("since", { element ->
    when (element) {
        is KdClass -> element.since
        is KdFunction -> element.since
        is KdProperty -> element.since
        is KdConstructor -> element.since
        is KdTypealias -> element.since
        is KdModule, is KdPackage, is KdRefinedElement, is KdInheritedMember -> null
    }
})

abstract class AbstractThrowsGoldenTest : AbstractElementFieldGoldenTest("throws", { element ->
    when (element) {
        is KdFunction -> element.throws
        is KdProperty -> element.throws
        is KdConstructor -> element.throws
        is KdModule, is KdPackage, is KdClass, is KdTypealias, is KdRefinedElement, is KdInheritedMember -> null
    }
})

abstract class AbstractOverridensGoldenTest : AbstractElementFieldGoldenTest("overridens", { element ->
    when (element) {
        is KdFunction -> element.overridens
        is KdProperty -> element.overridens
        is KdModule, is KdPackage, is KdClass, is KdConstructor, is KdTypealias, is KdRefinedElement, is KdInheritedMember -> null
    }
})

/** Prints every outermost [KdType] of an element with its path; nested types are part of their enclosing type's dump. */
abstract class AbstractKdmTypesGoldenTest : AbstractElementDumpGoldenTest() {
    override val extension: String get() = ".txt"

    override fun selectValues(element: KdElement): Map<String, Any?> = buildMap {
        fun typeParameters(typeParameters: List<KdTypeParameter>) = typeParameters.forEachIndexed { i, parameter ->
            parameter.upperBounds.forEachIndexed { j, bound -> put(".typeParameters[$i].upperBounds[$j]", bound) }
        }

        fun receiver(receiver: KdExtensionReceiverParameter?) {
            if (receiver != null) put(".extensionReceiverParameter.type", receiver.type)
        }

        fun contextParameters(parameters: List<KdContextParameter>) =
            parameters.forEachIndexed { i, parameter -> put(".contextParameters[$i].type", parameter.type) }

        fun valueParameters(parameters: List<KdValueParameter>) =
            parameters.forEachIndexed { i, parameter -> put(".valueParameters[$i].type", parameter.type) }

        when (element) {
            is KdClass -> {
                typeParameters(element.typeParameters)
                element.superTypes.forEachIndexed { i, type -> put(".superTypes[$i]", type) }
            }
            is KdFunction -> {
                typeParameters(element.typeParameters)
                receiver(element.extensionReceiverParameter)
                contextParameters(element.contextParameters)
                valueParameters(element.valueParameters)
                put(".returnType", element.returnType)
            }
            is KdProperty -> {
                typeParameters(element.typeParameters)
                receiver(element.extensionReceiverParameter)
                contextParameters(element.contextParameters)
                put(".type", element.type)
            }
            is KdConstructor -> {
                typeParameters(element.typeParameters)
                valueParameters(element.valueParameters)
            }
            is KdTypealias -> {
                typeParameters(element.typeParameters)
                put(".underlyingType", element.underlyingType)
            }
            is KdModule, is KdPackage, is KdRefinedElement, is KdInheritedMember -> {}
        }
    }
}
