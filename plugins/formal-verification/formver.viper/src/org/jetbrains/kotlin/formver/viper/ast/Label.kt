/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.MangledName

data class Label(
    val name: MangledName,
    val invariants: List<Exp>,
) {
    fun toStmt(
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ) = Stmt.Label(name, invariants, pos, info, trafos)

    fun toDecl(
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ) = Declaration.LabelDecl(name, invariants, pos, info, trafos)

    fun toGoto(
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ) = Stmt.Goto(name, pos, info, trafos)
}