/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.analysis.api.klib.reader.testUtils

import org.jetbrains.kotlin.native.analysis.api.*

fun Iterable<KlibDeclarationAddress>.render() = joinToString(System.lineSeparator().repeat(2)) { it.render() }

fun KlibDeclarationAddress.render(): String {
    return when (this) {
        is KlibFunctionAddress -> render()
        is KlibPropertyAddress -> render()
        is KlibClassAddress -> render()
        is KlibTypeAliasAddress -> render()
    }
}

private fun KlibFunctionAddress.render(): String = """
    Function (${packageFqName.child(callableName).asString()})
      Source File Name : "$sourceFileName"
      Package Name     : "${packageFqName.asString()}"
      Function Name    : "${callableName.asString()}"
""".trimIndent()

private fun KlibPropertyAddress.render(): String = """
    Property (${packageFqName.child(callableName).asString()})
      Source File Name : "$sourceFileName"
      Package Name     : "${packageFqName.asString()}"
      Property Name    : "${callableName.asString()}"
""".trimIndent()

private fun KlibClassAddress.render(): String = """
    Class (${classId.asFqNameString()})
      Source File Name : "$sourceFileName"
      Package Name     : "${packageFqName.asString()}"
      ClassId          : "${classId.asString()}"
""".trimIndent()

private fun KlibTypeAliasAddress.render(): String = """
    TypeAlias (${classId.asFqNameString()})
      Package Name     : "${packageFqName.asString()}"
      ClassId          : "${classId.asString()}"
""".trimIndent()

