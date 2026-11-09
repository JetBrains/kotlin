/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import kotlinx.serialization.json.Json
import org.jetbrains.kotlin.documentation.*
import org.jetbrains.kotlin.documentation.analyzer.internal.visitElementIds
import org.jetbrains.kotlin.analysis.test.data.manager.ManagedTest
import org.jetbrains.kotlin.analysis.test.data.manager.assertEqualsToTestDataFile
import java.io.File

/** Shared golden-file assertions for both Dokka Java analysis modes. */
abstract class AbstractKdmGoldenTest : ManagedTest {
    override val variantChain: List<String>
        get() = if (java.lang.Boolean.getBoolean("org.jetbrains.dokka.analysis.enableExperimentalSymbolsJavaAnalysis")) {
            listOf("java-symbols")
        } else {
            emptyList()
        }

    protected fun assertMatchesGoldenFile(bundle: KdBundle, testDataFile: File) {
        val actual = Json(from = KdJson) { prettyPrint = true }
            .encodeToString(KdBundle.serializer(), bundle) + "\n"
        assertMatchesGoldenTextFile(actual, testDataFile, ".kdm.json")
    }

    protected fun assertMatchesGoldenTextFile(actual: String, testDataFile: File, extension: String) {
        assertEqualsToTestDataFile(testDataFile.toPath(), actual, extension)
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

    fun resolveConcrete(startFragment: String, startId: KdElementId, startElement: KdElement): KdElement {
        var fragment = startFragment
        var id = startId
        var element = startElement
        while (true) {
            element = when (element) {
                is KdRefinedElement -> {
                    fragment = element.fragmentName
                    fragmentsByName.getValue(fragment).elements.getValue(id)
                }
                is KdInheritedMember -> {
                    id = element.originalId
                    val resolvedFragment = fragmentsByName.getValue(fragment)
                    resolvedFragment.elements[id] ?: resolvedFragment.embeddedElements.getValue(id)
                }
                else -> return element
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
        val element = fragment.elements.getValue(id)
        return resolveConcrete(fragment.name, id, element).signatureOrNull() ?: id.value
    }

    fun appendDeclaration(fragment: KdFragment, id: KdElementId, indent: String, out: StringBuilder, showStatus: Boolean = true) {
        val element = fragment.elements.getValue(id)
        val resolved = resolveConcrete(fragment.name, id, element)
        resolved.signatureOrNull()?.let { signature ->
            out.append(indent).append(signature)
            if (showStatus) {
                out.append(" (").append(statusKeyword(fragment, element)).append(')')
            }
            out.append('\n')
        }
        val children = when (resolved) {
            is KdClass -> resolved.declarations.sortedBy { sortKeyOf(fragment, it) }
            is KdProperty -> listOfNotNull(resolved.getter, resolved.setter)
            else -> emptyList()
        }
        for (childId in children) {
            appendDeclaration(fragment, childId, "$indent  ", out, showStatus && element !is KdRefinedElement)
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

internal fun KdElement.signatureOrNull(): String? = when (this) {
    is KdClass -> "${classKind.keyword()} $name"
    is KdConstructor -> "constructor(${valueParameters.joinToString(", ") { it.render() }})"
    is KdFunction -> "fun $name(${valueParameters.joinToString(", ") { it.render() }}): ${returnType.render()}"
    is KdProperty -> "${if (isMutable) "var" else "val"} $name: ${type.render()}"
    is KdTypealias -> "typealias $name = ${underlyingType.render()}"
    is KdModule, is KdPackage, is KdRefinedElement, is KdInheritedMember -> null
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

private fun KdTypeProjection.render(): String = when (this) {
    is KdTypeProjection.Invariant -> type.render()
    is KdTypeProjection.In -> "in ${type.render()}"
    is KdTypeProjection.Out -> "out ${type.render()}"
    KdTypeProjection.Star -> "*"
}
