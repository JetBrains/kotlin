/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

data class ObjCHeader(val lines: List<String>) {
    override fun toString(): String {
        return lines.joinToString(System.lineSeparator())
    }
}

fun ObjCHeader(
    stubs: List<ObjCExportStub>,
    classForwardDeclarations: Set<ObjCClassForwardDeclaration>,
    protocolForwardDeclarations: Set<String>,
    additionalImports: List<String> = emptyList(),
    exportKDoc: Boolean = false,
): ObjCHeader = ObjCHeader(buildList {
    addImports(foundationImports)
    addImports(additionalImports)
    add("")

    if (classForwardDeclarations.isNotEmpty()) {
        add("@class ${
            classForwardDeclarations.joinToString {
                buildString {
                    append(it.className)
                    formatGenerics(this, it.typeDeclarations)
                }
            }
        };")
        add("")
    }

    if (protocolForwardDeclarations.isNotEmpty()) {
        add("@protocol ${protocolForwardDeclarations.joinToString()};")
        add("")
    }

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

    stubs.forEach {
        addAll(StubRenderer.render(it, exportKDoc))
        add("")
    }

    add("#pragma pop_macro(\"$objcNullableResultAttribute\")")
    add("#pragma clang diagnostic pop")
    add("NS_ASSUME_NONNULL_END")
})

private fun MutableList<String>.addImports(imports: Iterable<String>) {
    imports.forEach {
        add("#import <$it>")
    }
}

private val foundationImports = listOf(
    "Foundation/NSArray.h",
    "Foundation/NSDictionary.h",
    "Foundation/NSError.h",
    "Foundation/NSObject.h",
    "Foundation/NSSet.h",
    "Foundation/NSString.h",
    "Foundation/NSValue.h"
)
