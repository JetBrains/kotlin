/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

import org.jetbrains.kotlin.formver.scala.emptySeq
import org.jetbrains.kotlin.formver.scala.seqOf
import org.jetbrains.kotlin.formver.scala.silicon.ast.*
import org.jetbrains.kotlin.formver.scala.toScalaBigInt
import viper.silver.ast.Program


class Converter {
    val program: Program
        get() = Program(
            emptySeq(), /* Domains */
            seqOf(
                field("foo", Type.Int),
                field("bar", Type.Ref),
                field("baz", Type.Bool),
                field("numbers", Type.Set(Type.Int)),
                field("numbers", Type.Set(Type.Int))
            ), /* Fields */
            seqOf(
                function(
                    name = "answerTheQuestion",
                    formalArgs = emptyList(),
                    type = Type.Int,
                    pres = emptyList(),
                    posts = emptyList(),
                    body = Exp.IntLit(42.toScalaBigInt())
                )
            ), /* Functions */
            emptySeq(), /* Predicates */
            seqOf(
                method(
                    name = "gaussSum",
                    formalArgs = listOf(localVarDecl("n", Type.Int)),
                    formalReturns = listOf(localVarDecl("s", Type.Int)),
                    pres = listOf(
                        Exp.GeCmp(Exp.LocalVar("n", Type.Int), Exp.IntLit(0.toScalaBigInt()))
                    ),
                    posts = listOf(
                        Exp.EqCmp(
                            Exp.LocalVar("s", Type.Int),
                            Exp.Div(
                                Exp.Mul(
                                    Exp.LocalVar("n", Type.Int),
                                    Exp.Add(Exp.LocalVar("n", Type.Int), Exp.IntLit(1.toScalaBigInt())),
                                ),
                                Exp.IntLit(2.toScalaBigInt())
                            )
                        )
                    ),
                    body = Stmt.Seqn(
                        stmts = listOf(
                            Stmt.LocalVarDeclStmt(localVarDecl("i", Type.Int)),
                            Stmt.LocalVarAssign(Exp.LocalVar("i", Type.Int), Exp.IntLit(0.toScalaBigInt())),
                            Stmt.While(
                                cond = Exp.LeCmp(Exp.LocalVar("i", Type.Int), Exp.LocalVar("n", Type.Int)),
                                body = Stmt.Seqn(
                                    stmts = listOf(
                                        Stmt.LocalVarAssign(
                                            lhs = Exp.LocalVar("i", Type.Int),
                                            rhs = Exp.Add(Exp.LocalVar("i", Type.Int), Exp.IntLit(1.toScalaBigInt()))
                                        ),
                                        Stmt.LocalVarAssign(
                                            lhs = Exp.LocalVar("s", Type.Int),
                                            rhs = Exp.Add(Exp.LocalVar("s", Type.Int), Exp.LocalVar("i", Type.Int))
                                        )
                                    ),
                                    scopedStmtsDeclaration = emptyList()
                                ),
                                invariants = listOf(
                                    Exp.EqCmp(
                                        Exp.LocalVar("s", Type.Int),
                                        Exp.Div(
                                            Exp.Mul(
                                                Exp.LocalVar("i", Type.Int),
                                                Exp.Add(Exp.LocalVar("i", Type.Int), Exp.IntLit(1.toScalaBigInt())),
                                            ),
                                            Exp.IntLit(2.toScalaBigInt())
                                        )
                                    )
                                )
                            ),
                        ),
                        scopedStmtsDeclaration = emptyList()
                    )
                )
            ), /* Methods */
            emptySeq(), /* Extensions */
            Position.NoPosition.toViper(),
            Info.NoInfo.toViper(),
            Trafos.NoTrafos.toViper()
        )
}