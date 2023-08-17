/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.scala.silicon.ast

import org.jetbrains.kotlin.formver.scala.*
import scala.math.BigInt
import viper.silver.ast.*

sealed interface Exp : IntoViper<viper.silver.ast.Exp> {

    //region Arithmetic Expressions
    data class Add(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.Add =
            Add(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Sub(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.Sub =
            Sub(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Mul(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.Mul =
            Mul(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Div(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.Div =
            Div(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Mod(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.Mod =
            Mod(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }
    //endregion

    //region Integer Comparison Expressions
    data class LtCmp(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.LtCmp =
            LtCmp(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class LeCmp(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.LeCmp =
            LeCmp(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class GtCmp(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.GtCmp =
            GtCmp(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class GeCmp(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.GeCmp =
            GeCmp(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }
    //endregion

    //region Boolean Comparison Expressions
    data class EqCmp(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.EqCmp =
            EqCmp(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class NeCmp(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.NeCmp =
            NeCmp(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }
    //endregion

    //region Boolean Expressions
    data class And(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.And =
            And(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Or(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.Or =
            Or(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Implies(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.Implies =
            Implies(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Not(
        val arg: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.Not =
            Not(arg.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    class Trigger(
        val exps: List<Exp>,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : IntoViper<viper.silver.ast.Trigger> {
        override fun toViper(): viper.silver.ast.Trigger =
            Trigger(exps.toViper().toScalaSeq(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Forall(
        val variables: List<Declaration.LocalVarDecl>,
        val triggers: List<Trigger>,
        val exp: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.Forall =
            Forall(
                variables.map { it.toViper() }.toScalaSeq(),
                triggers.toViper().toScalaSeq(),
                exp.toViper(),
                pos.toViper(),
                info.toViper(),
                trafos.toViper()
            )
    }

    data class Exists(
        val variables: List<Declaration.LocalVarDecl>,
        val triggers: List<Trigger>,
        val exp: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.Exists =
            Exists(
                variables.map { it.toViper() }.toScalaSeq(),
                triggers.toViper().toScalaSeq(),
                exp.toViper(),
                pos.toViper(),
                info.toViper(),
                trafos.toViper()
            )
    }
    //endregion

    //region Literals
    data class IntLit(
        val value: BigInt,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.IntLit = IntLit(value, pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class NullLit(
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.NullLit = NullLit(pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class BoolLit(
        val value: Boolean,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.BoolLit =
            viper.silver.ast.BoolLit.apply(value, pos.toViper(), info.toViper(), trafos.toViper())
    }
    //endregion

    data class LocalVar(
        val name: MangledName,
        val type: Type,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.LocalVar =
            LocalVar(name.mangled, type.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class FieldAccess(
        val rcv: Exp,
        val field: Field,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.FieldAccess =
            FieldAccess(rcv.toViper(), field.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Result(
        val type: Type,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.Result = Result(type.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    /**
     * IMPORTANT: typeVarMap needs to be set even when the type variables are
     * not instantiated. In that case map the generic type variables to themselves.
     * Example: x is of type T, f(x: T) -> Int is a domain function, and you want to
     * make the generic domain function call f(x) then a Map from T -> T needs to be
     * supplied.
     */
    data class DomainFuncApp(
        val funcname: DomainFuncName,
        val args: List<Exp>,
        val typeVarMap: Map<Type.TypeVar, Type>,
        val typ: Type,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        private val scalaTypeVarMap = typeVarMap.mapKeys { it.key.toViper() }.mapValues { it.value.toViper() }.toScalaMap()
        override fun toViper(): viper.silver.ast.Exp =
            DomainFuncApp(
                funcname.mangled,
                args.toViper().toScalaSeq(),
                scalaTypeVarMap,
                pos.toViper(),
                info.toViper(),
                typ.toViper().substitute(scalaTypeVarMap),
                funcname.domainName.mangled,
                trafos.toViper()
            )
    }
}