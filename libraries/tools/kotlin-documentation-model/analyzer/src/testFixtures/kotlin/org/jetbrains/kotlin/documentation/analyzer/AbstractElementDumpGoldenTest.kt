/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import org.jetbrains.kotlin.documentation.*
import kotlin.reflect.KProperty1
import kotlin.reflect.KVisibility
import kotlin.reflect.full.hasAnnotation

/**
 * Golden tests printing selected parts of every [KdElement] in every fragment, including embedded declarations.
 * Each entry is printed as `signature [element ID]<path>:` followed by a [KdDumper] dump of the value.
 * Expected output is stored beside the fixture, with variants where Java analysis modes differ.
 */
abstract class AbstractElementDumpGoldenTest : AbstractKdmGoldenTest() {
    protected abstract val extension: String

    /** Returns the values to print for [element], keyed by their path relative to the element, in print order. */
    protected abstract fun selectValues(element: KdElement): Map<String, Any?>

    fun runTest(testDataFilePath: String) {
        val file = testDataFile(testDataFilePath)
        val bundle = generateDocumentationBundleFromTestData(file)
        val actual = buildString {
            for (fragment in bundle.fragments) {
                appendLine("fragment ${fragment.name}")
                for ((mapName, elements) in listOf("elements" to fragment.elements, "embeddedElements" to fragment.embeddedElements)) {
                    for ((id, element) in elements) {
                        val values = selectValues(element)
                        if (values.isEmpty()) continue
                        val signature = when (element) {
                            is KdModule -> "module ${element.name}"
                            is KdPackage -> "package ${element.name.ifEmpty { "<root>" }}"
                            is KdRefinedElement -> "KdRefinedElement"
                            is KdInheritedMember -> "KdInheritedMember"
                            is KdClass, is KdConstructor, is KdFunction, is KdProperty, is KdTypealias -> element.signatureOrNull()
                        }
                        val prefix = if (mapName == "embeddedElements") "$mapName: " else ""
                        for ((path, value) in values) {
                            appendLine("$prefix$signature [${id.value}]$path:")
                            KdDumper.dump(value, this)
                            appendLine()
                        }
                        appendLine()
                    }
                }
            }
        }
        assertMatchesGoldenTextFile(actual, file, extension)
    }
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
    override val extension: String get() = ".types.txt"

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

/**
 * Text dump of KDM values: `Name { field = value }` blocks, indented lists, and quoted strings.
 * Model objects are dumped with their public properties, sorted by name, via Kotlin reflection (like `KaDebugSymbolRenderer.renderByPropertyNames`);
 * empty [KdDocumentation] fields are omitted, since they carry no content yet.
 */
class KdDumper private constructor(private val out: StringBuilder) {
    private var indent = ""

    private fun value(value: Any?) {
        when (value) {
            null -> out.append("null")
            is String -> out.append('"').append(value.escape()).append('"')
            is KdElementId -> value(value.value)
            is Enum<*> -> out.append(value.name)
            is List<*> -> list(value)
            is Map<*, *> -> list(value.entries.map { (key, item) -> MapEntry(key.toString(), item) })
            is MapEntry -> fields(value.key, listOf("value" to value.value))
            else -> obj(value)
        }
    }

    private class MapEntry(val key: String, val value: Any?)

    private fun obj(value: Any) {
        val kClass = value::class
        if (kClass.java.`package`?.name != MODEL_PACKAGE) {
            out.append(value)
            return
        }
        val fields = kClass.members
            .asSequence()
            .filterIsInstance<KProperty1<Any, *>>()
            .filter { it.visibility == KVisibility.PUBLIC && !it.hasAnnotation<Deprecated>() }
            .sortedBy { it.name }
            .map { it.name to it.get(value) }
            .filterNot { it.second is KdDocumentation }
            .toList()
        val name = kClass.simpleName!!
        if (fields.isEmpty()) out.append(name) else fields(name, fields)
    }

    private fun fields(name: String, fields: List<Pair<String, Any?>>) {
        out.append(name).append(" {\n")
        val outer = indent
        indent = "$outer  "
        for ((fieldName, value) in fields) {
            out.append(indent).append(fieldName).append(" = ")
            value(value)
            out.append('\n')
        }
        indent = outer
        out.append(indent).append('}')
    }

    private fun list(items: List<*>) {
        if (items.isEmpty()) {
            out.append("[]")
            return
        }
        out.append("[\n")
        val outer = indent
        indent = "$outer  "
        for (item in items) {
            out.append(indent)
            value(item)
            out.append('\n')
        }
        indent = outer
        out.append(indent).append(']')
    }

    companion object {
        private const val MODEL_PACKAGE = "org.jetbrains.kotlin.documentation"

        /** Appends the dump of [value] to [out]. */
        fun dump(value: Any?, out: StringBuilder) {
            KdDumper(out).value(value)
        }

        private fun String.escape(): String =
            replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t")
    }
}
