/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport.sx

import org.jetbrains.kotlin.backend.konan.objcexport.*

class SXObjCHeaderTextRenderer(
        private val shouldExportKDoc: Boolean,
) {

    fun render(header: SXObjCHeader): List<String> {
        val output = mutableListOf<String>()

        renderImportsTo(collectUniqueImports(header), output)

        renderClassForwardDeclarationsTo(header, output)

        if (header.protocolForwardDeclarations.isNotEmpty()) {
            output.add("@protocol ${header.protocolForwardDeclarations.joinToString()};")
            output.add("")
        }

        renderHeaderPreludeTo(output)

        header.topLevelDeclarations.forEach {
            output.addAll(StubRenderer.render(it, shouldExportKDoc))
            output.add("")
        }

        renderHeaderEpilogueTo(output)

        return output.toList()
    }

    private fun renderClassForwardDeclarationsTo(header: SXObjCHeader, output: MutableList<String>) {
        if (header.classForwardDeclarations.isNotEmpty()) {
            output.add("@class ${
                header.classForwardDeclarations.joinToString {
                    buildString {
                        append(it.className)
                        formatGenerics(this, it.typeDeclarations)
                    }
                }
            };")
            output.add("")
        }
    }

    private fun renderHeaderPreludeTo(output: MutableList<String>) = output.apply {
        add("NS_ASSUME_NONNULL_BEGIN")
        add("#pragma clang diagnostic push")
        listOf(
                "-Wunknown-warning-option",

                // Protocols don't have generics, classes do. So generated header may contain
                // overriding property with "incompatible" type, e.g. `Generic<T>`-typed property
                // overriding `Generic<id>`. Suppress these warnings:
                "-Wincompatible-property-type",

                "-Wnullability"
        ).forEach {
            add("#pragma clang diagnostic ignored \"$it\"")
        }
        add("")

        // If _Nullable_result is not supported, then use _Nullable:
        add("#pragma push_macro(\"$objcNullableResultAttribute\")")
        add("#if !__has_feature(nullability_nullable_result)")
        add("#undef $objcNullableResultAttribute")
        add("#define $objcNullableResultAttribute $objcNullableAttribute")
        add("#endif")
        add("")
    }

    private fun renderHeaderEpilogueTo(output: MutableList<String>) = output.apply {
        add("#pragma pop_macro(\"$objcNullableResultAttribute\")")
        add("#pragma clang diagnostic pop")
        add("NS_ASSUME_NONNULL_END")
    }

    private fun collectUniqueImports(header: SXObjCHeader): Set<String> =
            (header.imports + header.headerImports.map { it.name }).toSet()

    private fun renderImportsTo(imports: Set<String>, output: MutableList<String>) {
        imports.map {
            output += "#import <$it>"
        }
    }
}