/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package generators.test

import templates.Family
import templates.Family.*
import templates.PrimitiveType
import java.io.BufferedWriter
import java.io.File

fun collectionClassName(family: Family, primitive: PrimitiveType?): String = when (family) {
    Iterables, Sequences -> family.toString()
    ArraysOfObjects -> "Array"
    ArraysOfPrimitives, ArraysOfUnsigned -> "${primitive!!}Array"
    else -> error(family)
}

fun constructorName(family: Family, primitive: PrimitiveType?): String = when (family) {
    Iterables -> "listOf"
    Sequences -> "sequenceOf"
    ArraysOfObjects -> "arrayOf"
    ArraysOfPrimitives, ArraysOfUnsigned -> "${primitive!!.name.lowercase()}ArrayOf"
    else -> error(family)
}

fun forEachCollectionFamily(action: (Family, PrimitiveType?) -> Unit) {
    action(Iterables, null)
    action(Sequences, null)
    action(ArraysOfObjects, null)
    for (primitive in PrimitiveType.defaultPrimitives) {
        action(ArraysOfPrimitives, primitive)
    }
    for (primitive in PrimitiveType.unsignedPrimitives) {
        action(ArraysOfUnsigned, primitive)
    }
}

fun writeGeneratedFile(path: String, block: BufferedWriter.() -> Unit) {
    val file = File(path)
    file.parentFile.mkdirs()
    file.bufferedWriter().use { it.block() }
}

fun emptyCollectionExpr(ctor: String, primitive: PrimitiveType?): String =
    if (primitive == null) "$ctor<String>()" else "$ctor()"
