/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*
import viper.silver.ast.*

sealed interface Exp : IntoViper<viper.silver.ast.Exp> {

    val type: Type

    //region Arithmetic Expressions
    data class Add(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Int
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
        override val type = Type.Int
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
        override val type = Type.Int
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
        override val type = Type.Int
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
        override val type = Type.Int
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
        override val type = Type.Bool
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
        override val type = Type.Bool
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
        override val type = Type.Bool
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
        override val type = Type.Bool
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
        override val type = Type.Bool
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
        override val type = Type.Bool
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
        override val type = Type.Bool
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
        override val type = Type.Bool
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
        override val type = Type.Bool
        override fun toViper(): viper.silver.ast.Implies =
            Implies(left.toViper(), right.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Not(
        val arg: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Bool
        override fun toViper(): viper.silver.ast.Not =
            Not(arg.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }
    //endregion

    //region Quantifier Expressions
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
        override val type = Type.Bool
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
        override val type = Type.Bool
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

    companion object {
        fun Trigger1(
            exp: Exp,
            pos: Position = Position.NoPosition,
            info: Info = Info.NoInfo,
            trafos: Trafos = Trafos.NoTrafos,
        ): Trigger = Trigger(listOf(exp), pos, info, trafos)

        fun Forall1(
            variable: Declaration.LocalVarDecl,
            trigger: Trigger,
            exp: Exp,
            pos: Position = Position.NoPosition,
            info: Info = Info.NoInfo,
            trafos: Trafos = Trafos.NoTrafos,
        ): Forall = Forall(listOf(variable), listOf(trigger), exp, pos, info, trafos)

        fun Forall1(
            variable: Declaration.LocalVarDecl,
            exp: Exp,
            pos: Position = Position.NoPosition,
            info: Info = Info.NoInfo,
            trafos: Trafos = Trafos.NoTrafos,
        ): Forall = Forall(listOf(variable), emptyList(), exp, pos, info, trafos)


        fun Exists1(
            variable: Declaration.LocalVarDecl,
            trigger: Trigger,
            exp: Exp,
            pos: Position = Position.NoPosition,
            info: Info = Info.NoInfo,
            trafos: Trafos = Trafos.NoTrafos,
        ): Exists = Exists(listOf(variable), listOf(trigger), exp, pos, info, trafos)
    }
    //endregion

    //region Literals
    data class IntLit(
        val value: Int,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Int
        override fun toViper(): viper.silver.ast.IntLit = IntLit(value.toScalaBigInt(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class NullLit(
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Ref
        override fun toViper(): viper.silver.ast.NullLit = NullLit(pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class BoolLit(
        val value: Boolean,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Bool
        override fun toViper(): viper.silver.ast.BoolLit =
            viper.silver.ast.BoolLit.apply(value, pos.toViper(), info.toViper(), trafos.toViper())
    }
    //endregion

    data class LocalVar(
        val name: MangledName,
        override val type: Type,
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
        override val type = field.type
        override fun toViper(): viper.silver.ast.FieldAccess =
            FieldAccess(rcv.toViper(), field.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class Result(
        override val type: Type,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.Result = Result(type.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class FuncApp(
        val functionName: MangledName,
        val args: List<Exp>,
        override val type: Type,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toViper(): viper.silver.ast.FuncApp = FuncApp(
            functionName.mangled,
            args.toViper().toScalaSeq(),
            pos.toViper(),
            info.toViper(),
            type.toViper(),
            trafos.toViper()
        )
    }

    /**
     * IMPORTANT: typeVarMap needs to be set even when the type variables are
     * not instantiated. In that case map the generic type variables to themselves.
     * Example: x is of type T, f(x: T) -> Int is a domain function, and you want to
     * make the generic domain function call f(x) then a Map from T -> T needs to be
     * supplied.
     */
    data class DomainFuncApp(
        val function: DomainFunc,
        val args: List<Exp>,
        val typeVarMap: Map<Type.TypeVar, Type>,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        private val scalaTypeVarMap = typeVarMap.mapKeys { it.key.toViper() }.mapValues { it.value.toViper() }.toScalaMap()
        override val type = function.returnType.substitute(typeVarMap)
        override fun toViper(): viper.silver.ast.Exp =
            viper.silver.ast.DomainFuncApp.apply(
                function.toViper(),
                args.toViper().toScalaSeq(),
                scalaTypeVarMap,
                pos.toViper(),
                info.toViper(),
                trafos.toViper()
            )
    }

    data class Old(
        val exp: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = exp.type
        override fun toViper(): viper.silver.ast.Old = Old(exp.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }

    // We can't pass all the available position, info, and trafos information here.
    // Living with that seems fine for the moment.
    fun fieldAccess(
        field: Field,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): FieldAccess =
        FieldAccess(this, field, pos, info, trafos)

    // We can't pass all the available position, info, and trafos information here.
    // Living with that seems fine for the moment.
    fun fieldAccessPredicate(
        field: Field,
        permission: PermExp,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): AccessPredicate.FieldAccessPredicate =
        AccessPredicate.FieldAccessPredicate(fieldAccess(field), permission, pos, info, trafos)
}