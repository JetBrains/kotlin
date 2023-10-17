/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*
import viper.silver.ast.*

sealed interface Exp : IntoSilver<viper.silver.ast.Exp> {

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
        override fun toSilver(): viper.silver.ast.Add =
            Add(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class Sub(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Int
        override fun toSilver(): viper.silver.ast.Sub =
            Sub(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class Mul(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Int
        override fun toSilver(): viper.silver.ast.Mul =
            Mul(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class Div(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Int
        override fun toSilver(): viper.silver.ast.Div =
            Div(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class Mod(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Int
        override fun toSilver(): viper.silver.ast.Mod =
            Mod(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
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
        override fun toSilver(): viper.silver.ast.LtCmp =
            LtCmp(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class LeCmp(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Bool
        override fun toSilver(): viper.silver.ast.LeCmp =
            LeCmp(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class GtCmp(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Bool
        override fun toSilver(): viper.silver.ast.GtCmp =
            GtCmp(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class GeCmp(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Bool
        override fun toSilver(): viper.silver.ast.GeCmp =
            GeCmp(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
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
        override fun toSilver(): viper.silver.ast.EqCmp =
            EqCmp(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class NeCmp(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Bool
        override fun toSilver(): viper.silver.ast.NeCmp =
            NeCmp(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
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
        override fun toSilver(): viper.silver.ast.And =
            And(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class Or(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Bool
        override fun toSilver(): viper.silver.ast.Or =
            Or(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class Implies(
        val left: Exp,
        val right: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Bool
        override fun toSilver(): viper.silver.ast.Implies =
            Implies(left.toSilver(), right.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class Not(
        val arg: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Bool
        override fun toSilver(): viper.silver.ast.Not =
            Not(arg.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }
    //endregion

    //region Quantifier Expressions
    class Trigger(
        val exps: List<Exp>,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : IntoSilver<viper.silver.ast.Trigger> {
        override fun toSilver(): viper.silver.ast.Trigger =
            Trigger(exps.toSilver().toScalaSeq(), pos.toSilver(), info.toSilver(), trafos.toSilver())
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
        override fun toSilver(): viper.silver.ast.Forall =
            Forall(
                variables.map { it.toSilver() }.toScalaSeq(),
                triggers.toSilver().toScalaSeq(),
                exp.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                trafos.toSilver()
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
        override fun toSilver(): viper.silver.ast.Exists =
            Exists(
                variables.map { it.toSilver() }.toScalaSeq(),
                triggers.toSilver().toScalaSeq(),
                exp.toSilver(),
                pos.toSilver(),
                info.toSilver(),
                trafos.toSilver()
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
        override fun toSilver(): viper.silver.ast.IntLit = IntLit(value.toScalaBigInt(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class NullLit(
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Ref
        override fun toSilver(): viper.silver.ast.NullLit = NullLit(pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class BoolLit(
        val value: Boolean,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = Type.Bool
        override fun toSilver(): viper.silver.ast.BoolLit =
            viper.silver.ast.BoolLit.apply(value, pos.toSilver(), info.toSilver(), trafos.toSilver())
    }
    //endregion

    data class LocalVar(
        val name: MangledName,
        override val type: Type,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toSilver(): viper.silver.ast.LocalVar =
            LocalVar(name.mangled, type.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class FieldAccess(
        val rcv: Exp,
        val field: Field,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = field.type
        override fun toSilver(): viper.silver.ast.FieldAccess =
            FieldAccess(rcv.toSilver(), field.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class Result(
        override val type: Type,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toSilver(): viper.silver.ast.Result = Result(type.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class FuncApp(
        val functionName: MangledName,
        val args: List<Exp>,
        override val type: Type,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override fun toSilver(): viper.silver.ast.FuncApp = FuncApp(
            functionName.mangled,
            args.toSilver().toScalaSeq(),
            pos.toSilver(),
            info.toSilver(),
            type.toSilver(),
            trafos.toSilver()
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
        private val scalaTypeVarMap = typeVarMap.mapKeys { it.key.toSilver() }.mapValues { it.value.toSilver() }.toScalaMap()
        override val type = function.returnType.substitute(typeVarMap)
        override fun toSilver(): viper.silver.ast.Exp =
            viper.silver.ast.DomainFuncApp.apply(
                function.toSilver(),
                args.toSilver().toScalaSeq(),
                scalaTypeVarMap,
                pos.toSilver(),
                info.toSilver(),
                trafos.toSilver()
            )
    }

    data class Old(
        val exp: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = exp.type
        override fun toSilver(): viper.silver.ast.Old = Old(exp.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
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
        AccessPredicate.FieldAccessPredicate(fieldAccess(field, pos), permission, pos, info, trafos)
}