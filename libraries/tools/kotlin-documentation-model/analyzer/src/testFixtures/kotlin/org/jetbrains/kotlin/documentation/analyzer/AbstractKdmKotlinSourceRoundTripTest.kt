/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import kotlinx.serialization.json.JsonPrimitive
import org.jetbrains.kotlin.documentation.*
import kotlin.test.assertEquals

/**
 * The input is also the golden file: canonical, declaration-only Kotlin must survive analysis and rendering.
 * Bodies, imports, comments and original formatting are not represented by KDM and cannot be round-tripped.
 */
abstract class AbstractKdmKotlinSourceRoundTripTest {
    fun runTest(testDataFilePath: String) {
        val file = testDataFile(testDataFilePath)
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
                    else -> element
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

private fun KdAnnotation.renderSource(): String =
    classLikeId.value.split('/')[1] + if (arguments.isEmpty()) "" else
        arguments.entries.joinToString(", ", "(", ")") { entry -> "${entry.key} = ${entry.value.renderSource()}" }

private fun KdAnnotationArgumentValue.renderSource(): String = when (this) {
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

/** Canonical declaration-only source used by the round-trip fixtures. */
private fun KdFragment.renderKotlinSource(declarationModifiers: Map<KdElement, String>): String = buildString {
    fun typeParameters(parameters: List<KdTypeParameter>): String =
        if (parameters.isEmpty()) "" else parameters.joinToString(", ", "<", ">") { parameter ->
            val variance = parameter.variance?.name?.lowercase()?.plus(" ").orEmpty()
            val bounds = parameter.upperBounds.filterNot { it.render() == "Any?" }
            require(bounds.size <= 1) { "Multiple bounds need a where clause" }
            (if (parameter.isReified) "reified " else "") + variance + parameter.name +
                    bounds.singleOrNull()?.let { " : ${it.render()}" }.orEmpty()
        }

    fun receiver(parameter: KdExtensionReceiverParameter?): String =
        parameter?.let { "${it.type.render()}." }.orEmpty()

    fun name(element: KdElement): String = when (element) {
        is KdClass -> element.name
        is KdFunction -> element.name
        is KdProperty -> element.name
        is KdTypealias -> element.name
        else -> error("Unsupported source declaration: $element")
    }

    fun sourceDeclarations(ids: List<KdElementId>): List<KdElement> =
        ids.map { elements.getValue(it) }.filterNot {
            it is KdInheritedMember || it is KdConstructor && it.isPrimary || it is KdProperty && it.isFromPrimaryConstructor
        }.sortedBy { name(it).lowercase() }

    fun valueParameter(parameter: KdValueParameter): String {
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

    fun appendContextParameters(parameters: List<KdContextParameter>, indent: String) {
        if (parameters.isEmpty()) return
        append(parameters.joinToString(", ", "context(", ")\n") { parameter ->
            "${parameter.name ?: "_"}: ${parameter.type.render()}"
        })
        append(indent)
    }

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
                else -> emptyList()
            }
            annotations.forEach { append('@').append(it.renderSource()).append('\n').append(indent) }
            val modifier = declarationModifiers[element].orEmpty()
            if (!inExpectClass || modifier != "expect ") append(modifier)
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
                            else -> false
                        })
                    }
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
                    append(": ").append(element.returnType.render()).append('\n')
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
                    append('\n')
                }
                is KdTypealias -> {
                    append("typealias ").append(element.name).append(typeParameters(element.typeParameters))
                    append(" = ").append(element.underlyingType.render()).append('\n')
                }
                else -> error("Unsupported source declaration: $element")
            }
        }
    }

    elements.values.filterIsInstance<KdPackage>().sortedBy { it.name }.forEachIndexed { index, pkg ->
        require(index == 0) { "A Kotlin source file can only contain one package" }
        if (pkg.name.isNotEmpty()) append("package ").append(pkg.name).append("\n\n")
        appendDeclarations(pkg.declarations, "")
    }
}
