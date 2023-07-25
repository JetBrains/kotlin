/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.scala.silicon.ast

import org.jetbrains.kotlin.formver.scala.IntoViper
import scala.math.BigInt
import viper.silver.ast.*

sealed class Exp : IntoViper<viper.silver.ast.Exp> {

    //region Arithmetic Expressions
    data class Add(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ) : Exp() {
        override fun toViper(): viper.silver.ast.Exp =
            Add(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Sub(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ) : Exp() {
        override fun toViper(): viper.silver.ast.Exp =
            Sub(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Mul(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ) : Exp() {
        override fun toViper(): viper.silver.ast.Exp =
            Mul(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Div(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ) : Exp() {
        override fun toViper(): viper.silver.ast.Exp =
            Div(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Mod(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ) : Exp() {
        override fun toViper(): viper.silver.ast.Exp =
            Mod(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }
    //endregion

    //region Integer comparison Expressions
    data class LtCmp(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ) : Exp() {
        override fun toViper(): viper.silver.ast.Exp =
            LtCmp(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class LeCmp(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ) : Exp() {
        override fun toViper(): viper.silver.ast.Exp =
            LeCmp(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class GtCmp(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ) : Exp() {
        override fun toViper(): viper.silver.ast.Exp =
            GtCmp(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class GeCmp(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ) : Exp() {
        override fun toViper(): viper.silver.ast.Exp =
            GeCmp(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }
    //endregion

    //region Boolean Comparison Expressions
    data class EqCmp(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ) : Exp() {
        override fun toViper(): viper.silver.ast.Exp =
            EqCmp(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class NeCmp(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ) : Exp() {
        override fun toViper(): viper.silver.ast.Exp =
            NeCmp(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }
    //endregion

    //region Boolean Expressions
    data class And(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ) : Exp() {
        override fun toViper(): viper.silver.ast.Exp =
            And(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Or(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ) : Exp() {
        override fun toViper(): viper.silver.ast.Exp =
            Or(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Implies(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ) : Exp() {
        override fun toViper(): viper.silver.ast.Exp =
            Implies(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }
    //endregion

    data class IntLit(
        val value: BigInt,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ) : Exp() {
        override fun toViper(): viper.silver.ast.Exp = IntLit(value, pos.toViper(), info.toViper(), trafos.toViper())
    }


    data class LocalVar(
        val name: String,
        val type: Type,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ) : Exp() {
        override fun toViper(): viper.silver.ast.Exp = LocalVar(name, type.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Result(
        val type: Type,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos
    ) : Exp() {
        override fun toViper(): viper.silver.ast.Exp = Result(type.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

}