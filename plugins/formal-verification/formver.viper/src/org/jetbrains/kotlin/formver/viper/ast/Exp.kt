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

    data class PredicateAccess(
        val predicateName: MangledName,
        val formalArgs: List<Exp>,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        // The type is set to Bool just to be consistent with Silver type. Probably it will never be used
        override val type: Type = Type.Bool

        // Note: since the simple syntax P(...) has the same meaning as acc(P(...)), which in turn has the same meaning as acc(P(...), write)
        // It is always better to deal with PredicateAccessPredicate because PredicateAccess seems not working well with Silver
        override fun toSilver(): PredicateAccessPredicate {
            val predicateAccess = PredicateAccess(
                formalArgs.toSilver().toScalaSeq(),
                predicateName.mangled,
                pos.toSilver(),
                info.toSilver(),
                trafos.toSilver()
            )
            return PredicateAccessPredicate(
                predicateAccess,
                PermExp.FullPerm().toSilver(),
                pos.toSilver(),
                info.toSilver(),
                trafos.toSilver()
            )
        }
    }

    data class Unfolding(
        val predicateAccess: PredicateAccess,
        val body: Exp,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Exp {
        override val type = body.type
        override fun toSilver(): viper.silver.ast.Unfolding =
            Unfolding(predicateAccess.toSilver(), body.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
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

    companion object {
        private fun forallImpl(vars: List<Declaration.LocalVarDecl>, action: ForallBuilder.() -> Exp): Exp {
            val builder = ForallBuilder(vars)
            val body = builder.action()
            return builder.toForallExp(body)
        }

        /**
         * Create an Exp.Forall node, passing the local variables as local variable access expressions into the action.
         */
        fun forall(v1: Var, action: ForallBuilder.(LocalVar) -> Exp): Exp = forallImpl(listOf(v1.decl())) { action(v1.use()) }

        fun forall(v1: Var, v2: Var, action: ForallBuilder.(LocalVar, LocalVar) -> Exp): Exp =
            forallImpl(listOf(v1.decl(), v2.decl())) { action(v1.use(), v2.use()) }

        fun forall(v1: Var, v2: Var, v3: Var, action: ForallBuilder.(LocalVar, LocalVar, LocalVar) -> Exp): Exp =
            forallImpl(listOf(v1.decl(), v2.decl(), v3.decl())) { action(v1.use(), v2.use(), v3.use()) }

        /**
         * Take the conjunction of the given expressions.
         */
        fun List<Exp>.toConjunction(): Exp =
            if (isEmpty()) BoolLit(true)
            else reduce { l, r -> And(l, r) }

    }

    /**
     * Builder for statements of the form
     * ```
     * forall vars :: { triggers } assumptions ==> conclusion
     * ```
     *
     * The assumptions and conclusion together are the body of the forall.
     *
     * This class is intended to be used via the `Exp.forall` functions, not directly.
     */
    class ForallBuilder(private val vars: List<Declaration.LocalVarDecl>) {
        private val triggers = mutableListOf<Trigger>()
        private val assumptions = mutableListOf<Exp>()

        fun toForallExp(conclusion: Exp): Exp {
            val body =
                if (assumptions.isNotEmpty()) {
                    Implies(assumptions.toConjunction(), conclusion)
                } else {
                    conclusion
                }
            return Forall(vars, triggers, body)
        }

        /**
         * Add an assumption to this forall statement.
         */
        fun assumption(action: () -> Exp): Exp {
            val exp = action()
            assumptions.add(exp)
            return exp
        }

        /**
         * Create a trigger consisting of a single expression.
         */
        fun simpleTrigger(action: () -> Exp): Exp {
            val exp = action()
            triggers.add(Trigger(listOf(exp)))
            return exp
        }

        /**
         * Create a trigger consisting of multiple expressions, and return them as a conjunction.
         *
         * Note that a compound trigger must contain at least one expression; an empty list does
         * not make sense here.
         */
        fun compoundTrigger(action: TriggerBuilder.() -> Unit): Exp {
            val builder = TriggerBuilder()
            builder.action()
            triggers.add(builder.toTrigger())
            return builder.toConjunction()
        }

        class TriggerBuilder {
            private val exps = mutableListOf<Exp>()

            /**
             * Add an expression to the trigger.
             */
            fun subTrigger(action: () -> Exp): Exp {
                val exp = action()
                exps.add(exp)
                return exp
            }

            fun toTrigger(): Trigger {
                assert(exps.isNotEmpty()) { "There is no point to having an empty trigger expression. " }
                return Trigger(exps)
            }

            fun toConjunction(): Exp = exps.toConjunction()
        }
    }
}