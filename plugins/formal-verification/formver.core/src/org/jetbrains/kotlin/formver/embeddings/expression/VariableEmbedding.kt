/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.fir.symbols.FirBasedSymbol
import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.asSourceRole
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.embeddings.*
import org.jetbrains.kotlin.formver.embeddings.expression.debug.NamedBranchingNode
import org.jetbrains.kotlin.formver.embeddings.expression.debug.PlaintextLeaf
import org.jetbrains.kotlin.formver.embeddings.expression.debug.TreeView
import org.jetbrains.kotlin.formver.names.AnonymousName
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.*

sealed interface VariableEmbedding : PureExpEmbedding, PropertyAccessEmbedding {
    val name: MangledName
    override val type: TypeEmbedding

    fun toLocalVarDecl(
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Declaration.LocalVarDecl = Declaration.LocalVarDecl(name, type.viperType, pos, info, trafos)

    fun toLocalVarUse(
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Exp.LocalVar = Exp.LocalVar(name, type.viperType, pos, info, trafos)

    override fun toViper(source: KtSourceElement?): Exp.LocalVar = Exp.LocalVar(name, type.viperType, source.asPosition, sourceRole.asInfo)

    override fun getValue(ctx: StmtConversionContext): ExpEmbedding = this
    override fun setValue(value: ExpEmbedding, ctx: StmtConversionContext): ExpEmbedding = Assign(this, value)

    fun pureInvariants(): List<ExpEmbedding> = type.pureInvariants().fillHoles(this)
    fun provenInvariants(): List<ExpEmbedding> = type.provenInvariants().fillHoles(this)
    fun accessInvariants(): List<ExpEmbedding> = type.accessInvariants().fillHoles(this)
    fun dynamicInvariants(): List<ExpEmbedding> = type.dynamicInvariants().fillHoles(this)

    override val debugTreeView: TreeView
        get() = NamedBranchingNode("Var", PlaintextLeaf(name.mangled))
}

/**
 * Embedding of a variable that is only used as a local placeholder, e.g. the return value or parameters
 * in a type signature.
 */
class PlaceholderVariableEmbedding(override val name: MangledName, override val type: TypeEmbedding) : VariableEmbedding

/**
 * Embedding of an anonymous variable.
 */
class AnonymousVariableEmbedding(n: Int, override val type: TypeEmbedding) : VariableEmbedding {
    override val name: MangledName = AnonymousName(n)
}

/**
 * Embedding of a variable that comes from some FIR element.
 */
class FirVariableEmbedding(override val name: MangledName, override val type: TypeEmbedding, val symbol: FirBasedSymbol<*>) :
    VariableEmbedding {
    override val sourceRole: SourceRole
        get() = symbol.asSourceRole
}

/**
 * Variable embedding generated at linearization phase.
 *
 * This can still correspond to an earlier variable, but it no longer carries any interesting information.
 */
class LinearizationVariableEmbedding(override val name: MangledName, override val type: TypeEmbedding) : VariableEmbedding