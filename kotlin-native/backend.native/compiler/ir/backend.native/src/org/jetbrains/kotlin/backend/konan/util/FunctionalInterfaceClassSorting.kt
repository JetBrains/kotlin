/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.konan.util

import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile

/**
 * This utility is used to sort synthetic `*Function` classes in a special function interface file inside the standard library.
 * The classes might be generated and added to the file on demand and in the different order (based on the order or the deserialization queue).
 * So, we need to sort them explicitly after the IR linkage stage to keep the stable order.
 */
internal fun sortDeclarationsInFunctionInterfaceFile(file: IrFile) {
    val sortedDeclarations: List<FunctionalInterfaceClassSortingKey> = file.declarations
            .map { FunctionalInterfaceClassSortingKey(it as IrDeclarationWithName) }
            .sorted()
    file.declarations.clear()
    sortedDeclarations.mapTo(file.declarations) { it.declaration }
}

private class FunctionalInterfaceClassSortingKey(val declaration: IrDeclarationWithName) : Comparable<FunctionalInterfaceClassSortingKey> {
    val prefix: String
    val index: Int

    init {
        val name = declaration.name.asString()
        prefix = name.trimEnd { it.isDigit() }
        index = name.substringAfter(prefix).toIntOrNull() ?: -1
    }

    override fun compareTo(other: FunctionalInterfaceClassSortingKey): Int {
        val prefixDiff = prefix.compareTo(other.prefix)
        return if (prefixDiff != 0) prefixDiff else index.compareTo(other.index)
    }
}
