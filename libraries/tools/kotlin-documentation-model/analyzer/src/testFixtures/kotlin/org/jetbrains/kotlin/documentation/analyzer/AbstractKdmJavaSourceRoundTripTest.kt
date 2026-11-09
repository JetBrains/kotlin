/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.documentation.analyzer

import org.jetbrains.kotlin.documentation.*
import kotlin.test.assertEquals

/**
 * Canonical, declaration-only Java is reconstructed from KDM.
 * Fixtures cover interfaces and abstract classes without explicit constructors, bodies, imports or comments.
 */
abstract class AbstractKdmJavaSourceRoundTripTest {
    fun runTest(testDataFilePath: String) {
        val file = testDataFile(testDataFilePath)
        val modules = parseTestDataModules(file)
        val bundle = generateDocumentationBundleFromTestData(file)
        modules.forEach { module ->
            val source = module.files.values.single().trimEnd() + "\n"
            val fragment = bundle.fragments.single { it.name == module.name }
            assertEquals(source, fragment.renderJavaSource(), "Java source round-trip: ${file.name}, ${module.name}")
        }
    }
}

private fun KdType.renderJavaSource(): String = when (this) {
    is KdFlexibleType -> lowerBound.renderJavaSource()
    is KdTypeParameterType -> name
    is KdClassLikeType -> {
        qualifiers.joinToString(".") { qualifier ->
            (if (qualifier.name == "Unit") "void" else qualifier.name) + if (qualifier.typeArguments.isEmpty()) "" else
                qualifier.typeArguments.joinToString(", ", "<", ">") { projection ->
                    when (projection) {
                        is KdTypeProjection.Invariant -> projection.type.renderJavaSource()
                        is KdTypeProjection.In -> "? super ${projection.type.renderJavaSource()}"
                        is KdTypeProjection.Out -> "? extends ${projection.type.renderJavaSource()}"
                        KdTypeProjection.Star -> "?"
                    }
                }
        }
    }
    else -> error("Unsupported Java type: $this")
}

private fun List<KdTypeParameter>.renderJavaTypeParameters(): String =
    if (isEmpty()) "" else joinToString(", ", "<", ">") { parameter ->
        val bounds = parameter.upperBounds.map { it.renderJavaSource() }.filterNot { it == "Any" || it == "Object" }
        parameter.name + if (bounds.isEmpty()) "" else bounds.joinToString(" & ", " extends ")
    }

private fun KdVisibility.renderJavaVisibility(): String = when (this) {
    KdVisibility.PUBLIC -> "public "
    KdVisibility.PROTECTED, KdVisibility.PACKAGE_PROTECTED -> "protected "
    KdVisibility.PRIVATE -> "private "
    KdVisibility.PACKAGE_PRIVATE -> ""
    KdVisibility.INTERNAL -> error("Java declarations cannot be internal")
}

private fun KdFragment.renderJavaSource(): String = buildString {
    val pkg = elements.values.filterIsInstance<KdPackage>().single()
    if (pkg.name.isNotEmpty()) append("package ").append(pkg.name).append(";\n\n")
    val declaration = elements.getValue(pkg.declarations.single()) as KdClass
    val isInterface = declaration.classKind == KdClassKind.INTERFACE
    require(isInterface || declaration.classKind == KdClassKind.CLASS && declaration.modality == KdModality.ABSTRACT)
    append(declaration.visibility.renderJavaVisibility())
    append(if (isInterface) "interface " else "abstract class ")
    append(declaration.name).append(declaration.typeParameters.renderJavaTypeParameters()).append(" {\n")
    val methods = declaration.declarations.map { elements.getValue(it) }
        .filterNot { it is KdInheritedMember || it is KdConstructor }
    methods.sortedBy { (it as KdFunction).name.lowercase() }.forEachIndexed { index, element ->
        val method = element as KdFunction
        require(method.modality == KdModality.ABSTRACT)
        if (index > 0) append('\n')
        append("    ")
        if (!isInterface) append(method.visibility.renderJavaVisibility()).append("abstract ")
        val typeParameters = method.typeParameters.renderJavaTypeParameters()
        if (typeParameters.isNotEmpty()) append(typeParameters).append(' ')
        append(method.returnType.renderJavaSource()).append(' ').append(method.name)
        append(method.valueParameters.joinToString(", ", "(", ");\n") { parameter ->
            "${parameter.type.renderJavaSource()} ${parameter.name}"
        })
    }
    append("}\n")
}
