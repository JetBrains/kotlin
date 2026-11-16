/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.documentation.*
import kotlin.test.assertEquals

/**
 * The input is also the golden file: canonical, declaration-only Kotlin must survive analysis and rendering.
 * Bodies, imports, comments and original formatting are not represented by KDM and cannot be round-tripped.
 */
abstract class AbstractKdmKotlinSourceRoundTripTest {
    fun runTest(testDataFilePath: String) {
        val file = ForTestCompileRuntime.transformTestDataPath(testDataFilePath)
        val modules = parseTestDataModules(file)
        val bundle = generateDocumentationBundleFromTestData(file).withExpandedRefinedMembers()
        val fragments = bundle.fragments.associateBy { it.name }
        fun dependencies(name: String): Set<String> =
            modules.single { it.name == name }.dependsOn.flatMapTo(mutableSetOf()) { dependencies(it) + it }

        fun resolve(fragmentName: String, id: KdElementId): KdElement {
            val element = fragments.getValue(fragmentName).elements.getValue(id)
            return if (element is KdRefinedElement) resolve(element.fragmentName, id) else element
        }
        modules.forEach { module ->
            val fragment = fragments.getValue(module.name)
            val modifiers = buildMap {
                bundle.expectActualMappings.forEach { mapping ->
                    mapping.expectIds[module.name]?.takeIf {
                        dependencies(module.name).none { it in mapping.expectIds }
                    }?.let { put(it, "expect ") }
                    mapping.actualIds[module.name]?.takeIf {
                        dependencies(module.name).none { it in mapping.actualIds }
                    }?.let { put(it, "actual ") }
                }
            }
            val inheritedMultiplatformIds = bundle.expectActualMappings.flatMap { mapping ->
                listOfNotNull(mapping.expectIds[module.name], mapping.actualIds[module.name])
            }.toSet() - modifiers.keys
            // Common classes are reanalyzed and materialized in dependent fragments too.
            // Their presence here does not mean they were declared in this module.
            val dependencyIds = dependencies(module.name).flatMapTo(mutableSetOf()) {
                fragments.getValue(it).elements.keys
            }
            val declaredIds = (fragment.elements.filterValues { it !is KdRefinedElement }.keys - dependencyIds + modifiers.keys) -
                    inheritedMultiplatformIds
            val resolved = fragment.copy(elements = fragment.elements.mapValues { entry ->
                when (val element = resolve(module.name, entry.key)) {
                    is KdPackage -> element.copy(declarations = element.declarations.filter { it in declaredIds })
                    is KdModule -> element
                    is KdClass -> element
                    is KdFunction -> element
                    is KdConstructor -> element
                    is KdProperty -> element
                    is KdTypealias -> element
                    is KdRefinedElement -> element
                    is KdInheritedMember -> element
                }
            })
            val declarationModifiers = modifiers.entries.associate { entry -> resolved.elements.getValue(entry.key) to entry.value }
            val source = module.files.values.single().trimEnd() + "\n"
            assertEquals(
                source, resolved.renderKotlinSource(declarationModifiers),
                "Kotlin source round-trip: ${file.name}, ${module.name}",
            )
        }
    }
}

/** Canonical declaration-only source used by the round-trip fixtures. */
private fun KdFragment.renderKotlinSource(declarationModifiers: Map<KdElement, String>): String = buildString {
    fun name(element: KdElement): String = when (element) {
        is KdClass -> element.name
        is KdFunction -> element.name
        is KdProperty -> element.name
        is KdTypealias -> element.name
        is KdConstructor, is KdPackage, is KdModule, is KdRefinedElement, is KdInheritedMember ->
            error("Unsupported source declaration: $element")
    }

    fun sourceDeclarations(ids: List<KdElementId>): List<KdElement> =
        ids.map { elements.getValue(it) }.filterNot {
            it is KdInheritedMember || it is KdConstructor && it.isPrimary || it is KdProperty && it.isFromPrimaryConstructor
        }.sortedBy { name(it).lowercase() }

    fun appendDeclarations(ids: List<KdElementId>, indent: String, inClass: Boolean = false, inExpectClass: Boolean = false) {
        // Inherited declarations are not source declarations.
        val declarations = sourceDeclarations(ids)
        declarations.forEachIndexed { index, element ->
            if (index > 0) append('\n')
            append(indent)
            val annotations = when (element) {
                is KdClass -> element.annotations
                is KdFunction -> element.annotations
                is KdProperty -> element.annotations
                is KdTypealias -> element.annotations
                is KdConstructor, is KdPackage, is KdModule, is KdRefinedElement, is KdInheritedMember -> emptyList()
            }
            annotations.forEach { append('@').append(it.renderSource()).append('\n').append(indent) }
            val modifier = declarationModifiers[element].orEmpty()
            if (!inExpectClass || modifier != "expect ") append(modifier)
            when (element) {
                is KdClass -> {
                    appendSignature(element, elements, indent)
                    val members = element.declarations.map { elements.getValue(it) }
                    val enumEntries = if (element.classKind == KdClassKind.ENUM_CLASS) {
                        members.filterIsInstance<KdProperty>().filter {
                            it.getter == null && (it.type as? KdClassLikeType)?.classLikeId?.let(elements::get) == element
                        }
                    } else emptyList()
                    // Enum helpers are generated by the compiler, rather than declared in the source.
                    val declarationIds = element.declarations.filter { id ->
                        val member = elements.getValue(id)
                        member !in enumEntries && !(element.classKind == KdClassKind.ENUM_CLASS && when (member) {
                            is KdProperty -> member.name == "entries"
                            is KdFunction -> member.name == "values" && member.valueParameters.isEmpty() ||
                                    member.name == "valueOf" && member.valueParameters.singleOrNull()?.type?.render() == "String"
                            is KdClass -> false
                            is KdConstructor -> false
                            is KdTypealias -> false
                            is KdPackage -> false
                            is KdModule -> false
                            is KdRefinedElement -> false
                            is KdInheritedMember -> false
                        })
                    }
                    if (enumEntries.isEmpty() && sourceDeclarations(declarationIds).isEmpty()) {
                        append('\n')
                    } else {
                        append(" {\n")
                        enumEntries.forEachIndexed { entryIndex, entry ->
                            append(indent).append("    ")
                            entry.annotations.forEach { append('@').append(it.renderSource()).append(' ') }
                            append(entry.name).append(if (entryIndex == enumEntries.lastIndex) ";\n" else ",\n")
                        }
                        if (enumEntries.isNotEmpty() && sourceDeclarations(declarationIds).isNotEmpty()) append('\n')
                        appendDeclarations(
                            declarationIds, "$indent    ", element.classKind in setOf(KdClassKind.CLASS, KdClassKind.ENUM_CLASS),
                            inExpectClass || modifier == "expect ",
                        )
                        append(indent).append("}\n")
                    }
                }
                is KdFunction, is KdProperty -> {
                    appendSignature(element, elements, indent, inClass)
                    append('\n')
                }
                is KdTypealias -> {
                    appendSignature(element, elements, indent)
                    append('\n')
                }
                is KdConstructor, is KdPackage, is KdModule, is KdRefinedElement, is KdInheritedMember ->
                    error("Unsupported source declaration: $element")
            }
        }
    }

    elements.values.filterIsInstance<KdPackage>().sortedBy { it.name }.forEachIndexed { index, pkg ->
        require(index == 0) { "A Kotlin source file can only contain one package" }
        if (pkg.name.isNotEmpty()) append("package ").append(pkg.name).append("\n\n")
        appendDeclarations(pkg.declarations, "")
    }
}
