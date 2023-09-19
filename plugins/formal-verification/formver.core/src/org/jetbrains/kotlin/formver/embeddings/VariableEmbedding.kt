/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.embeddings

import org.jetbrains.kotlin.formver.conversion.ResultTrackingContext
import org.jetbrains.kotlin.formver.conversion.StmtConversionContext
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.ast.*

class VariableEmbedding(val name: MangledName, override val type: TypeEmbedding) : ExpEmbedding, PropertyAccessEmbedding {

    fun toLocalVarDecl(
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Declaration.LocalVarDecl = Declaration.LocalVarDecl(name, type.viperType, pos, info, trafos)


    override fun toViper(): Exp.LocalVar = Exp.LocalVar(name, type.viperType)

    fun toFieldAccess(
        field: Field,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Exp.FieldAccess = Exp.FieldAccess(toViper(), field, pos, info, trafos)

    override fun getValue(ctx: StmtConversionContext<ResultTrackingContext>): ExpEmbedding = this

    override fun setValue(value: ExpEmbedding, ctx: StmtConversionContext<ResultTrackingContext>) {
        ctx.addStatement(Stmt.LocalVarAssign(toViper(), value.withType(type).toViper()))
    }

    fun invariants(): List<Exp> = type.invariants(toViper())

    fun provenInvariants(): List<Exp> = type.provenInvariants(toViper())
    fun accessInvariants(): List<Exp> = type.accessInvariants(toViper())
    fun dynamicInvariants(): List<Exp> = type.dynamicInvariants(toViper())
}