/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.scala.silicon.ast

import org.jetbrains.kotlin.formver.scala.toScalaOption
import org.jetbrains.kotlin.formver.scala.toScalaSeq
import viper.silver.ast.*
import viper.silver.ast.Function

fun field(
    name: String,
    type: Type,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos
): Field = Field(name, type.toViper(), pos.toViper(), info.toViper(), trafos.toViper())

fun localVarDecl(
    name: String,
    type: Type,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos
): LocalVarDecl = LocalVarDecl(name, type.toViper(), pos.toViper(), info.toViper(), trafos.toViper())


fun function(
    name: String,
    formalArgs: List<LocalVarDecl>,
    type: Type,
    pres: List<Exp>,
    posts: List<Exp>,
    body: Exp?,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos
): Function = Function(
    name,
    formalArgs.toScalaSeq(),
    type.toViper(),
    pres.map { it.toViper() }.toScalaSeq(),
    posts.map { it.toViper() }.toScalaSeq(),
    body.toScalaOption().map { it.toViper() },
    pos.toViper(),
    info.toViper(),
    trafos.toViper()
)

fun method(
    name: String,
    formalArgs: List<LocalVarDecl>,
    formalReturns: List<LocalVarDecl>,
    pres: List<Exp>,
    posts: List<Exp>,
    body: Stmt.Seqn?,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos
) = Method(
    name,
    formalArgs.toScalaSeq(),
    formalReturns.toScalaSeq(),
    pres.map { it.toViper() }.toScalaSeq(),
    posts.map { it.toViper() }.toScalaSeq(),
    body.toScalaOption().map { it.toViper() as Seqn },
    pos.toViper(),
    info.toViper(),
    trafos.toViper()
)
