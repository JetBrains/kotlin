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

interface SirVisitor<Data, Result> {
    fun acceptModule(module: SirModule, data: Data): Result
    fun acceptForeignFunction(function: SirForeignFunction, data: Data): Result
    fun acceptSwiftFunction(function: SirFunction, data: Data): Result
    fun acceptType(type: SirType, data: Data): Result
    fun acceptParameter(param: SirParameter, data: Data): Result
}

data class SirModule(
    val declarations: MutableList<SirDeclaration> = mutableListOf()
) : SirElement {
    override fun <Data, Result> accept(
        visitor: SirVisitor<Data, Result>,
        data: Data
    ): Result = visitor.acceptModule(this, data)
}

data class SirType(val name: String) : SirDeclaration {
    override fun <Data, Result> accept(
        visitor: SirVisitor<Data, Result>,
        data: Data
    ): Result = visitor.acceptType(this, data)

}
data class SirForeignFunction(
    val fqName: List<String>
) : SirDeclaration {
    override fun <Data, Result> accept(
        visitor: SirVisitor<Data, Result>,
        data: Data
    ): Result = visitor.acceptForeignFunction(this, data)
}

data class SirFunction(
    val name: String,
    val arguments: MutableList<SirParameter> = mutableListOf(),
    val returnType: SirType
) : SirDeclaration {
    override fun <Data, Result> accept(
        visitor: SirVisitor<Data, Result>,
        data: Data
    ): Result = visitor.acceptSwiftFunction(this, data)
}

data class SirParameter(
    val name: String,
    val type: SirType
) : SirElement {
    override fun <Data, Result> accept(
        visitor: SirVisitor<Data, Result>,
        data: Data
    ): Result = visitor.acceptParameter(this, data)
}
