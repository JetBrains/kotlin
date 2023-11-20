/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.sir

/**
 * A marker interface that denotes Swift IR elements.
 */
interface SirElement {
    fun <Data, Result> accept(visitor: SirVisitor<Data, Result>, data: Data): Result
}

interface SirDeclaration : SirElement

// TODO: this is a mock-implementation. Should be replaced with actual implementation from KT-63266
interface SirVisitor<Data, Result> {
    fun visitModule(module: SirModule, data: Data): Result
    fun visitForeignFunction(function: SirForeignFunction, data: Data): Result
    fun visitSwiftFunction(function: SirFunction, data: Data): Result
    fun visitType(type: SirType, data: Data): Result
    fun visitParameter(param: SirParameter, data: Data): Result
}

data class SirModule(
    val declarations: MutableList<SirDeclaration> = mutableListOf()
) : SirElement {
    override fun <Data, Result> accept(
        visitor: SirVisitor<Data, Result>,
        data: Data
    ): Result = visitor.visitModule(this, data)
}

data class SirType(val name: String) : SirElement {
    override fun <Data, Result> accept(
        visitor: SirVisitor<Data, Result>,
        data: Data
    ): Result = visitor.visitType(this, data)

}
data class SirForeignFunction(
    val fqName: List<String>
) : SirDeclaration {
    override fun <Data, Result> accept(
        visitor: SirVisitor<Data, Result>,
        data: Data
    ): Result = visitor.visitForeignFunction(this, data)
}

data class SirFunction(
    val name: String,
    val parameters: MutableList<SirParameter> = mutableListOf(),
    val returnType: SirType
) : SirDeclaration {
    override fun <Data, Result> accept(
        visitor: SirVisitor<Data, Result>,
        data: Data
    ): Result = visitor.visitSwiftFunction(this, data)
}

data class SirParameter(
    val name: String,
    val type: SirType
) : SirElement {
    override fun <Data, Result> accept(
        visitor: SirVisitor<Data, Result>,
        data: Data
    ): Result = visitor.visitParameter(this, data)
}
