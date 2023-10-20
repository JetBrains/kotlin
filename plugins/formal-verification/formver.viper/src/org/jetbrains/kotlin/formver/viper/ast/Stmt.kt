/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.IntoSilver
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.toScalaSeq
import org.jetbrains.kotlin.formver.viper.toSilver

sealed interface Stmt : IntoSilver<viper.silver.ast.Stmt> {

    data class LocalVarAssign(
        val lhs: Exp.LocalVar,
        val rhs: Exp,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        override fun toSilver(): viper.silver.ast.LocalVarAssign =
            viper.silver.ast.LocalVarAssign(lhs.toSilver(), rhs.toSilver(), position.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class FieldAssign(
        val lhs: Exp.FieldAccess,
        val rhs: Exp,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        override fun toSilver(): viper.silver.ast.FieldAssign =
            viper.silver.ast.FieldAssign(lhs.toSilver(), rhs.toSilver(), position.toSilver(), info.toSilver(), trafos.toSilver())
    }

    companion object {
        fun assign(
            lhs: Exp,
            rhs: Exp,
            position: Position = Position.NoPosition,
            info: Info = Info.NoInfo,
            trafos: Trafos = Trafos.NoTrafos,
        ): Stmt = when (lhs) {
            is Exp.LocalVar ->
                LocalVarAssign(lhs, rhs, position, info, trafos)
            is Exp.FieldAccess ->
                FieldAssign(lhs, rhs, position, info, trafos)
            else -> throw IllegalArgumentException("Expected an lvalue on the left-hand side of an assignment, but lhs was ${lhs.toSilver()}.")
        }
    }

    data class MethodCall(
        val methodName: MangledName,
        val args: List<Exp>,
        val targets: List<Exp.LocalVar>,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        override fun toSilver(): viper.silver.ast.MethodCall = viper.silver.ast.MethodCall(
            methodName.mangled,
            args.map { it.toSilver() }.toScalaSeq(),
            targets.map { it.toSilver() }.toScalaSeq(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    data class Exhale(
        val exp: Exp,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        override fun toSilver(): viper.silver.ast.Exhale = viper.silver.ast.Exhale(
            exp.toSilver(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    data class Inhale(
        val exp: Exp,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        override fun toSilver(): viper.silver.ast.Inhale = viper.silver.ast.Inhale(
            exp.toSilver(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    data class Assert(
        val exp: Exp,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        override fun toSilver(): viper.silver.ast.Assert = viper.silver.ast.Assert(
            exp.toSilver(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    data class Seqn(
        val stmts: List<Stmt> = listOf(),
        val scopedSeqnDeclarations: List<Declaration> = listOf(),
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        override fun toSilver(): viper.silver.ast.Seqn = viper.silver.ast.Seqn(
            stmts.toSilver().toScalaSeq(),
            scopedSeqnDeclarations.toSilver().toScalaSeq(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    data class If(
        val cond: Exp,
        val then: Seqn,
        val els: Seqn,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        override fun toSilver(): viper.silver.ast.If = viper.silver.ast.If(
            cond.toSilver(),
            then.toSilver(),
            els.toSilver(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    data class While(
        val cond: Exp,
        val invariants: List<Exp>,
        val body: Seqn,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        override fun toSilver(): viper.silver.ast.While = viper.silver.ast.While(
            cond.toSilver(),
            invariants.map { it.toSilver() }.toScalaSeq(),
            body.toSilver(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    data class Label(
        val name: MangledName,
        val invariants: List<Exp>,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        override fun toSilver(): viper.silver.ast.Label = viper.silver.ast.Label(
            name.mangled,
            invariants.map { it.toSilver() }.toScalaSeq(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    data class Goto(
        val name: MangledName,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        override fun toSilver(): viper.silver.ast.Goto = viper.silver.ast.Goto(
            name.mangled,
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    data class Fold(
        val acc: Exp.PredicateAccess,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        override fun toSilver(): viper.silver.ast.Fold = viper.silver.ast.Fold(
            acc.toSilver(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }

    data class Unfold(
        val acc: Exp.PredicateAccess,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt {
        override fun toSilver(): viper.silver.ast.Unfold = viper.silver.ast.Unfold(
            acc.toSilver(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }
}