/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.IntoSilver
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.toScalaOption
import org.jetbrains.kotlin.formver.viper.toScalaSeq

class Predicate(
    val name: MangledName,
    val formalArgs: List<Declaration.LocalVarDecl>,
    val body: Exp,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<viper.silver.ast.Predicate> {
    override fun toSilver(): viper.silver.ast.Predicate =
        viper.silver.ast.Predicate(
            name.mangled,
            formalArgs.map { it.toSilver() }.toScalaSeq(),
            body.toSilver().toScalaOption(),
            pos.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
}