/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.scala.silicon.ast

import org.jetbrains.kotlin.formver.scala.IntoViper
import org.jetbrains.kotlin.formver.scala.toScalaSeq
import viper.silver.ast.Declaration
import viper.silver.ast.FieldAccess
import viper.silver.ast.LocalVar
import viper.silver.ast.LocalVarDecl

sealed class Stmt : IntoViper<viper.silver.ast.Stmt> {

    data class LocalVarAssign(
        val lhs: Exp.LocalVar,
        val rhs: Exp,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt() {
        override fun toViper(): viper.silver.ast.Stmt =
            viper.silver.ast.LocalVarAssign(lhs.toViper() as LocalVar, rhs.toViper(), position.toViper(), info.toViper(), trafos.toViper())
    }

    data class FieldAssign(
        val lhs: Exp.FieldAccess,
        val rhs: Exp,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt() {
        override fun toViper(): viper.silver.ast.Stmt =
            viper.silver.ast.FieldAssign(lhs.toViper() as FieldAccess, rhs.toViper(), position.toViper(), info.toViper(), trafos.toViper())
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
            else -> throw IllegalArgumentException("Expected an lvalue on the left-hand side of an assignment, but lhs was $lhs.")
        }
    }

    data class MethodCall(
        val methodName: String,
        val args: List<Exp>,
        val targets: List<Exp.LocalVar>,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt() {
        override fun toViper(): viper.silver.ast.Stmt = viper.silver.ast.MethodCall(
            methodName,
            args.map { it.toViper() }.toScalaSeq(),
            targets.map { it.toViper() as LocalVar }.toScalaSeq(),
            position.toViper(),
            info.toViper(),
            trafos.toViper()
        )
    }

    data class Exhale(
        val exp: Exp,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt() {
        override fun toViper(): viper.silver.ast.Stmt = viper.silver.ast.Exhale(
            exp.toViper(),
            position.toViper(),
            info.toViper(),
            trafos.toViper()
        )
    }

    data class Inhale(
        val exp: Exp,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt() {
        override fun toViper(): viper.silver.ast.Stmt = viper.silver.ast.Inhale(
            exp.toViper(),
            position.toViper(),
            info.toViper(),
            trafos.toViper()
        )
    }

    data class Assert(
        val exp: Exp,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt() {
        override fun toViper(): viper.silver.ast.Stmt = viper.silver.ast.Assert(
            exp.toViper(),
            position.toViper(),
            info.toViper(),
            trafos.toViper()
        )
    }

    data class Assume(
        val exp: Exp,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt() {
        override fun toViper(): viper.silver.ast.Stmt = viper.silver.ast.Assume(
            exp.toViper(),
            position.toViper(),
            info.toViper(),
            trafos.toViper()
        )
    }

    data class Seqn(
        val stmts: List<Stmt>,
        val scopedStmtsDeclaration: List<Declaration>,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt() {
        override fun toViper(): viper.silver.ast.Stmt = viper.silver.ast.Seqn(
            stmts.map { it.toViper() }.toScalaSeq(),
            scopedStmtsDeclaration.toScalaSeq(),
            position.toViper(),
            info.toViper(),
            trafos.toViper()
        )
    }

    data class If(
        val cond: Exp,
        val then: Seqn,
        val els: Seqn,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt() {
        override fun toViper(): viper.silver.ast.Stmt = viper.silver.ast.If(
            cond.toViper(),
            then.toViper() as viper.silver.ast.Seqn,
            els.toViper() as viper.silver.ast.Seqn,
            position.toViper(),
            info.toViper(),
            trafos.toViper()
        )
    }

    data class While(
        val cond: Exp,
        val invariants: List<Exp>,
        val body: Seqn,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt() {
        override fun toViper(): viper.silver.ast.Stmt = viper.silver.ast.While(
            cond.toViper(),
            invariants.map { it.toViper() }.toScalaSeq(),
            body.toViper() as viper.silver.ast.Seqn,
            position.toViper(),
            info.toViper(),
            trafos.toViper()
        )
    }

    data class Label(
        val name: String,
        val invariants: List<Exp>,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt() {
        override fun toViper(): viper.silver.ast.Stmt = viper.silver.ast.Label(
            name,
            invariants.map { it.toViper() }.toScalaSeq(),
            position.toViper(),
            info.toViper(),
            trafos.toViper()
        )
    }

    data class Goto(
        val name: String,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt() {
        override fun toViper(): viper.silver.ast.Stmt = viper.silver.ast.Goto(
            name,
            position.toViper(),
            info.toViper(),
            trafos.toViper()
        )
    }

    data class LocalVarDeclStmt(
        val decl: LocalVarDecl,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Stmt() {
        override fun toViper(): viper.silver.ast.Stmt = viper.silver.ast.LocalVarDeclStmt(
            decl,
            position.toViper(),
            info.toViper(),
            trafos.toViper()
        )
    }
}