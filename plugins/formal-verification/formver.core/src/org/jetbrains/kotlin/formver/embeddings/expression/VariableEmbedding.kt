/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings.expression

import org.jetbrains.kotlin.KtSourceElement
import org.jetbrains.kotlin.formver.asPosition
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.embeddings.PropertyAccessEmbedding
import org.jetbrains.kotlin.formver.embeddings.TypeEmbedding
import org.jetbrains.kotlin.formver.embeddings.expression.debug.NamedBranchingNode
import org.jetbrains.kotlin.formver.embeddings.expression.debug.PlaintextLeaf
import org.jetbrains.kotlin.formver.embeddings.expression.debug.TreeView
import org.jetbrains.kotlin.formver.embeddings.fillHoles
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.*

class VariableEmbedding(val name: MangledName, override val type: TypeEmbedding) :
    PureExpEmbedding,
    PropertyAccessEmbedding {

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

    override fun toViper(source: KtSourceElement?): Exp.LocalVar = Exp.LocalVar(name, type.viperType, source.asPosition)

    override fun getValue(ctx: StmtConversionContext): ExpEmbedding = this
    override fun setValue(value: ExpEmbedding, ctx: StmtConversionContext): ExpEmbedding = Assign(this, value)

    fun pureInvariants(): List<ExpEmbedding> = type.pureInvariants().fillHoles(this)
    fun provenInvariants(): List<ExpEmbedding> = type.provenInvariants().fillHoles(this)
    fun accessInvariants(): List<ExpEmbedding> = type.accessInvariants().fillHoles(this)
    fun dynamicInvariants(): List<ExpEmbedding> = type.dynamicInvariants().fillHoles(this)

    override val debugTreeView: TreeView
        get() = NamedBranchingNode("Var", PlaintextLeaf(name.mangled))
}