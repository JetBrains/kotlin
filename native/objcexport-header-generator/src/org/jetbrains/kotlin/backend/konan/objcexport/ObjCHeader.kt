/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.objcexport

import org.jetbrains.kotlin.backend.konan.InternalKotlinNativeApi

data class ObjCHeader(
    @property:InternalKotlinNativeApi val stubs: List<ObjCExportStub>,
    @property:InternalKotlinNativeApi val classForwardDeclarations: Set<ObjCClassForwardDeclaration>,
    @property:InternalKotlinNativeApi val protocolForwardDeclarations: Set<String>,
    @property:InternalKotlinNativeApi val additionalImports: List<String>,
) {

    fun renderClassForwardDeclarations(): List<String> = buildList {
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
    }

    fun renderProtocolForwardDeclarations(): List<String> = buildList {
        if (protocolForwardDeclarations.isNotEmpty()) {
            add("@protocol ${protocolForwardDeclarations.joinToString()};")
            add("")
        }
    }

    fun render(exportKDoc: Boolean = true): List<String> {
        return buildList {
            addImports(foundationImports)
            addImports(additionalImports)
            add("")

            addAll(renderClassForwardDeclarations())
            addAll(renderProtocolForwardDeclarations())

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
        }
    }

    override fun toString(): String {
        return render().joinToString(System.lineSeparator())
    }
}

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
