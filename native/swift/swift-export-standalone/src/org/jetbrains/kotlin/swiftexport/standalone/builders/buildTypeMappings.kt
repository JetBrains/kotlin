/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.swiftexport.standalone.builders

import org.jetbrains.kotlin.analysis.api.symbols.KaNamedClassSymbol
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.sir.SirClass
import org.jetbrains.kotlin.sir.SirDeclarationContainer
import org.jetbrains.kotlin.sir.allClasses
import org.jetbrains.kotlin.sir.allContainers
import org.jetbrains.kotlin.sir.providers.source.KotlinSource
import org.jetbrains.kotlin.sir.providers.utils.KotlinRuntimeModule

// Cannot be present in Kotlin identifier name: https://kotlinlang.org/spec/syntax-and-grammar.html#identifiers
private const val KOTLIN_SWIFT_NAME_SEPARATOR = "`"

internal data class TypeMapping(
    val kotlinFqName: FqName,
    val swiftBinaryName: String,
) {
    override fun toString() = "$kotlinFqName$KOTLIN_SWIFT_NAME_SEPARATOR$swiftBinaryName"
}

internal fun buildRuntimeTypeMappings(): Sequence<TypeMapping> = sequenceOf(
    TypeMapping(FqName("kotlin.Any"), KotlinRuntimeModule.kotlinBase.binaryName!!)
)

private val SirClass.typeMapping: TypeMapping?
    get() = (origin as? KotlinSource)?.let {
        val kotlinFqName = (it.symbol as KaNamedClassSymbol).classId!!.asSingleFqName() // Only named classes can be exported
        val swiftBinaryName = binaryName!! // Every exported class must have binary name
        TypeMapping(kotlinFqName, swiftBinaryName)
    }

internal fun buildTypeMappings(container: SirDeclarationContainer): Sequence<TypeMapping> =
    container.allClasses().mapNotNull { it.typeMapping } + container.allContainers().flatMap { buildTypeMappings(it) }