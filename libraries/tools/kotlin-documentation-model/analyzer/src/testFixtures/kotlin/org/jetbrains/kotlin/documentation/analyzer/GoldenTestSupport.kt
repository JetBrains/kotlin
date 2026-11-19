/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.kotlin.documentation.*
import org.jetbrains.kotlin.documentation.analyzer.internal.visitElementIds
import org.jetbrains.kotlin.test.services.JUnit5Assertions
import java.io.File
import kotlin.text.ifEmpty

/** Shared golden-file assertions for the golden test suites. */
abstract class AbstractKdmGoldenTest {
    protected fun assertMatchesGoldenFile(bundle: KdBundle, testDataFile: File) {
        val actual = json
            .encodeToString(KdBundle.serializer(), bundle) + "\n"
        assertMatchesGoldenTextFile(actual, testDataFile, ".kdm.json")
    }

    protected fun assertMatchesGoldenTextFile(actual: String, testDataFile: File, extension: String) {
        val expectedFile = File(testDataFile.parentFile, testDataFile.nameWithoutExtension + "." + extension.removePrefix("."))
        JUnit5Assertions.assertEqualsToFile(expectedFile, actual)
    }
}

/**
 * For each fragment, the signature of every visible declaration (classes, constructors, functions, properties, typealiases),
 * with members printed indented under their containing class - i.e. "what's visible from this fragment, laid out
 * the way it would read as source" - each tagged:
 * - `declared` if this fragment fully materializes it,
 * - `refined` if it's a [KdRefinedElement] pointing at another fragment that does,
 * - `inherited` if it's a [KdInheritedMember] pointing at an original declaration in the fragment, or
 * - `external` if it's a [KdInheritedMember] pointing at an embedded declaration from a dependency.
 * Property getters and setters are printed indented under their property when present in the model.
 * Members of a refined class or property are printed without status tags, including nested members.
 */
internal fun KdBundle.declarationSignaturesByFragment(): String {
    return withExpandedRefinedMembers().expandedDeclarationSignaturesByFragment()
}

/** Restores individual refinement entries for renderers that traverse a flat per-fragment map. */
internal fun KdBundle.withExpandedRefinedMembers(): KdBundle {
    val fragmentsByName = fragments.associateBy { it.name }
    return copy(fragments = fragments.map { fragment ->
        val elements = fragment.elements.toMutableMap()
        val visited = mutableSetOf<Pair<KdFragmentName, KdElementId>>()
        fun expand(fragmentName: KdFragmentName, id: KdElementId) {
            if (!visited.add(fragmentName to id)) return
            val source = fragmentsByName.getValue(fragmentName)
            val element = source.elements[id] ?: return
            if (element is KdRefinedElement) {
                expand(element.fragmentName, id)
            } else {
                visitElementIds(element, "") { childId, _ ->
                    if (childId !in elements && childId in source.elements) {
                        elements[childId] = KdRefinedElement(fragmentName)
                    }
                    val child = elements[childId]
                    if (child is KdRefinedElement) expand(child.fragmentName, childId)
                }
            }
        }
        fragment.elements.keys.forEach { id -> expand(fragment.name, id) }
        fragment.copy(elements = elements)
    })
}

private fun KdBundle.expandedDeclarationSignaturesByFragment(): String {
    val fragmentsByName = fragments.associateBy { it.name }
    // Signatures may mention members, which can live in either map of the fragment owning the element.
    val memberElements = fragmentsByName.mapValues {
        it.value.elements + (it.value.embeddedElements - it.value.elements.keys)
    }

    class Resolved(val fragment: KdFragment, val element: KdElement)

    fun resolveConcrete(startFragment: String, startId: KdElementId, startElement: KdElement): Resolved {
        var fragmentName = startFragment
        var id = startId
        var element = startElement
        while (true) {
            element = when (element) {
                is KdRefinedElement -> {
                    fragmentName = element.fragmentName
                    fragmentsByName.getValue(fragmentName).elements.getValue(id)
                }
                is KdInheritedMember -> {
                    id = element.originalId
                    val resolvedFragment = fragmentsByName.getValue(fragmentName)
                    resolvedFragment.elements[id] ?: resolvedFragment.embeddedElements.getValue(id)
                }
                else -> return Resolved(fragmentsByName.getValue(fragmentName), element)
            }
        }
    }

    fun statusKeyword(fragment: KdFragment, element: KdElement): String = when (element) {
        is KdRefinedElement -> "refined"
        is KdInheritedMember -> if (element.originalId in fragment.embeddedElements) "external" else "inherited"
        else -> "declared"
    }

    // The sort key is the rendered signature (falling back to the ID for signature-less elements),
    // not source/declaration order, so the output doesn't depend on incidental internal ordering.
    fun sortKeyOf(fragment: KdFragment, id: KdElementId): String {
        val resolved = resolveConcrete(fragment.name, id, fragment.elements.getValue(id))
        return resolved.element.sourceSignatureOrNull(memberElements.getValue(resolved.fragment.name)) ?: id.value
    }

    fun appendDeclaration(
        fragment: KdFragment,
        id: KdElementId,
        indent: String,
        out: StringBuilder,
        showStatus: Boolean = true,
        inClass: Boolean = false,
    ) {
        val element = fragment.elements.getValue(id)
        val resolved = resolveConcrete(fragment.name, id, element)
        resolved.element.sourceSignatureOrNull(memberElements.getValue(resolved.fragment.name), inClass)?.let { signature ->
            out.append(indent).append(signature)
            if (showStatus) {
                out.append(" (").append(statusKeyword(fragment, element)).append(')')
            }
            out.append('\n')
        }
        val children = when (resolved.element) {
            is KdClass -> resolved.element.declarations.sortedBy { sortKeyOf(fragment, it) }
            is KdProperty -> listOfNotNull(resolved.element.getter, resolved.element.setter)
            else -> emptyList()
        }
        for (childId in children) {
            appendDeclaration(fragment, childId, "$indent  ", out, showStatus && element !is KdRefinedElement, resolved.element is KdClass)
        }
    }

    return fragments.sortedBy { it.name }.joinToString("\n") { fragment ->
        val out = StringBuilder("${fragment.name}:\n")
        fragment.elements.values.filterIsInstance<KdPackage>()
            .sortedBy { it.name }
            .forEach { pkg ->
                for (id in pkg.declarations.sortedBy { sortKeyOf(fragment, it) }) {
                    appendDeclaration(fragment, id, "  ", out)
                }
            }
        out.toString()
    }
}

/** The full, source-grade signature rendered by [appendSignature], or null for non-declaration elements. */
internal fun KdElement.sourceSignatureOrNull(elements: Map<KdElementId, KdElement>, inClass: Boolean = false): String? =
    when (this) {
        is KdModule, is KdPackage, is KdRefinedElement, is KdInheritedMember -> null
        else -> buildString { appendSignature(this@sourceSignatureOrNull, elements, inClass = inClass) }
    }

/** A compact one-line signature used to label elements in golden dumps. */
internal fun KdElement.compactSignature(): String = when (this) {
    is KdClass -> "${classKind.keyword()} $name"
    is KdConstructor -> "constructor(${valueParameters.joinToString(", ") { it.render() }})"
    is KdFunction -> "fun $name(${valueParameters.joinToString(", ") { it.render() }}): ${returnType.render()}"
    is KdProperty -> "${if (isMutable) "var" else "val"} $name: ${type.render()}"
    is KdTypealias -> "typealias $name = ${underlyingType.render()}"
    is KdModule -> "module ${this.name}"
    is KdPackage -> "package ${this.name.ifEmpty { "<root>" }}"
    is KdRefinedElement -> "KdRefinedElement"
    is KdInheritedMember -> "KdInheritedMember"
}

private fun KdClassKind.keyword(): String = when (this) {
    KdClassKind.CLASS -> "class"
    KdClassKind.INTERFACE -> "interface"
    KdClassKind.OBJECT -> "object"
    KdClassKind.ENUM_CLASS -> "enum class"
    KdClassKind.ANNOTATION_CLASS -> "annotation class"
}

private fun KdValueParameter.render(): String = "$name: ${type.render()}"

internal fun KdType.render(): String = when (this) {
    is KdClassLikeType -> qualifiers.joinToString(".") { qualifier ->
        qualifier.name + if (qualifier.typeArguments.isEmpty()) "" else "<${qualifier.typeArguments.joinToString(", ") { it.render() }}>"
    } + (if (isMarkedNullable) "?" else "")
    is KdFunctionalType -> buildString {
        if (isMarkedNullable) append('(')
        if (contextParameterTypes.isNotEmpty()) {
            append("context(${contextParameterTypes.joinToString(", ") { it.render() }}) ")
        }
        if (isSuspend) append("suspend ")
        receiverType?.let { append(it.render()).append('.') }
        append("(${valueParameterTypes.joinToString(", ") { it.render() }}) -> ${returnType.render()}")
        if (isMarkedNullable) append(")?")
    }
    is KdTypeParameterType -> name + (if (isMarkedNullable) "?" else "")
    is KdDynamicType -> "dynamic"
    is KdFlexibleType -> "${lowerBound.render()}..${upperBound.render()}"
    is KdDefinitelyNotNullType -> "${original.render()} & Any"
    is KdErrorUnionType -> primaryType.render()
}

internal fun KdAnnotation.renderSource(): String =
    classLikeId.value.split('/')[1] + if (arguments.isEmpty()) "" else
        arguments.entries.joinToString(", ", "(", ")") { entry -> "${entry.key} = ${entry.value.renderSource()}" }

internal fun KdAnnotationArgumentValue.renderSource(): String = when (this) {
    is KdAnnotationArgumentValue.Annotation -> annotation.renderSource()
    is KdAnnotationArgumentValue.Array -> elements.joinToString(", ", "[", "]") { it.renderSource() }
    is KdAnnotationArgumentValue.Classlike -> classLikeId.value.split('/')[1] + "::class"
    is KdAnnotationArgumentValue.EnumEntry -> enumEntryId.value.split('/').let { parts ->
        parts[1] + parts[2].takeIf(String::isNotEmpty)?.let { name -> ".$name" }.orEmpty()
    }
    is KdAnnotationArgumentValue.Const -> when (val value = expression.value) {
        is KdConstValue.String -> JsonPrimitive(value.value).toString()
        is KdConstValue.Boolean -> value.value.toString()
        is KdConstValue.Int -> value.value.toString()
        is KdConstValue.Long -> "${value.value}L"
        is KdConstValue.Float -> "${value.value}f"
        is KdConstValue.Double -> value.value.toString()
        else -> error("Unsupported annotation constant: $value")
    }
}

private fun typeParameters(parameters: List<KdTypeParameter>): String =
    if (parameters.isEmpty()) "" else parameters.joinToString(", ", "<", ">") { parameter ->
        val variance = parameter.variance?.name?.lowercase()?.plus(" ").orEmpty()
        val bounds = parameter.upperBounds.filterNot { it.render() == "Any?" }
        require(bounds.size <= 1) { "Multiple bounds need a where clause" }
        (if (parameter.isReified) "reified " else "") + variance + parameter.name +
                bounds.singleOrNull()?.let { " : ${it.render()}" }.orEmpty()
    }

private fun receiver(parameter: KdExtensionReceiverParameter?): String =
    parameter?.let { "${it.type.render()}." }.orEmpty()

private fun valueParameter(parameter: KdValueParameter): String {
    val defaultValue = when (val expression = parameter.defaultValueExpression) {
        null -> ""
        is KdDefaultValueExpression.Defined -> " = ${expression.expression.text}"
        is KdDefaultValueExpression.Inherited -> error("Inherited defaults must not be rendered as explicit defaults")
    }
    return parameter.annotations.joinToString("") { "@${it.renderSource()} " } +
            (if (parameter.isNoinline) "noinline " else "") +
            (if (parameter.isCrossinline) "crossinline " else "") +
            (if (parameter.isVararg) "vararg " else "") + "${parameter.name}: ${parameter.type.render()}" + defaultValue
}

private fun StringBuilder.appendContextParameters(parameters: List<KdContextParameter>, indent: String) {
    if (parameters.isEmpty()) return
    append(parameters.joinToString(", ", "context(", ")\n") { parameter ->
        "${parameter.name ?: "_"}: ${parameter.type.render()}"
    })
    append(indent)
}

/**
 * Appends the pure signature of [element] — its declaration header without a body: no braces or members for
 * classes, no trailing newline. [elements] resolves members for primary-constructor properties, [indent]
 * lays out context parameters, and [inClass] adds member-specific modality prefixes.
 */
internal fun StringBuilder.appendSignature(
    element: KdElement,
    elements: Map<KdElementId, KdElement>,
    indent: String = "",
    inClass: Boolean = false,
) {
    when (element) {
        is KdClass -> {
            when (element.classKind) {
                KdClassKind.INTERFACE -> append("interface ")
                KdClassKind.ANNOTATION_CLASS -> append("annotation class ")
                KdClassKind.ENUM_CLASS -> append("enum class ")
                KdClassKind.OBJECT -> append(if (element.isCompanion) "companion object" else "object")
                KdClassKind.CLASS -> {
                    when (element.modality) {
                        KdModality.ABSTRACT -> append("abstract ")
                        KdModality.OPEN -> append("open ")
                        KdModality.SEALED -> append("sealed ")
                        KdModality.FINAL -> Unit
                    }
                    append("class ")
                }
            }
            if (element.classKind == KdClassKind.OBJECT) {
                if (!element.isCompanion || element.name != "Companion") append(' ').append(element.name)
            } else {
                append(element.name).append(typeParameters(element.typeParameters))
            }
            val members = element.declarations.mapNotNull(elements::get)
            val primaryConstructor = members.filterIsInstance<KdConstructor>().singleOrNull { it.isPrimary }
            if (primaryConstructor != null && primaryConstructor.valueParameters.isNotEmpty()) {
                val properties = members.filterIsInstance<KdProperty>()
                    .filter { it.isFromPrimaryConstructor }.associateBy { it.name }
                append(primaryConstructor.valueParameters.joinToString(", ", "(", ")") { parameter ->
                    val property = properties[parameter.name]
                    (property?.let { if (it.isMutable) "var " else "val " }.orEmpty()) + valueParameter(parameter)
                })
            }
            val superTypes = element.superTypes.filterNot {
                element.classKind == KdClassKind.ENUM_CLASS &&
                        (it as? KdClassLikeType)?.classLikeId?.value == "kotlin/Enum///PointingToDeclaration/"
            }
            if (superTypes.isNotEmpty()) {
                append(superTypes.joinToString(", ", " : ") { it.render() })
            }
        }
        is KdConstructor -> append("constructor(")
            .append(element.valueParameters.joinToString(", ", transform = ::valueParameter))
            .append(")")
        is KdFunction -> {
            appendContextParameters(element.contextParameters, indent)
            if (inClass && element.modality == KdModality.ABSTRACT) append("abstract ")
            if (inClass && element.modality == KdModality.OPEN) append("open ")
            if (inClass && element.overridens.isNotEmpty() && element.modality == KdModality.FINAL) append("final ")
            if (element.overridens.isNotEmpty()) append("override ")
            if (element.isExternal) append("external ")
            if (element.isInline) append("inline ")
            if (element.isTailRec) append("tailrec ")
            if (element.isSuspend) append("suspend ")
            if (element.isOperator) append("operator ")
            if (element.isInfix) append("infix ")
            append("fun ")
            val parameters = typeParameters(element.typeParameters)
            if (parameters.isNotEmpty()) append(parameters).append(' ')
            append(receiver(element.extensionReceiverParameter)).append(element.name)
            append(element.valueParameters.joinToString(", ", "(", ")", transform = ::valueParameter))
            append(": ").append(element.returnType.render())
        }
        is KdProperty -> {
            appendContextParameters(element.contextParameters, indent)
            if (inClass && element.modality == KdModality.ABSTRACT) append("abstract ")
            if (inClass && element.modality == KdModality.OPEN) append("open ")
            if (inClass && element.overridens.isNotEmpty() && element.modality == KdModality.FINAL) append("final ")
            if (element.overridens.isNotEmpty()) append("override ")
            if (element.isExternal) append("external ")
            if (element.isInline) append("inline ")
            if (element.isLateinit) append("lateinit ")
            if (element.constExpression != null) append("const ")
            append(if (element.isMutable) "var " else "val ")
            val parameters = typeParameters(element.typeParameters)
            if (parameters.isNotEmpty()) append(parameters).append(' ')
            append(receiver(element.extensionReceiverParameter)).append(element.name)
            append(": ").append(element.type.render())
            element.constExpression?.let { append(" = ").append(requireNotNull(it.expression).text) }
        }
        is KdTypealias -> {
            append("typealias ").append(element.name).append(typeParameters(element.typeParameters))
            append(" = ").append(element.underlyingType.render())
        }
        is KdPackage, is KdModule, is KdRefinedElement, is KdInheritedMember ->
            error("Unsupported source declaration: $element")
    }
}

private fun KdTypeProjection.render(): String = when (this) {
    is KdTypeProjection.Invariant -> type.render()
    is KdTypeProjection.In -> "in ${type.render()}"
    is KdTypeProjection.Out -> "out ${type.render()}"
    KdTypeProjection.Star -> "*"
}
