/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.IntoSilver
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.toScalaSeq
import org.jetbrains.kotlin.formver.viper.toSilver

sealed interface Declaration : IntoSilver<viper.silver.ast.Declaration> {
    data class LocalVarDecl(
        val name: MangledName,
        val type: Type,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Declaration {
        override fun toSilver(): viper.silver.ast.LocalVarDecl =
            viper.silver.ast.LocalVarDecl(name.mangled, type.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
    }

    data class LabelDecl(
        val name: MangledName,
        val invariants: List<Exp>,
        val position: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Declaration {
        override fun toSilver(): viper.silver.ast.Label = viper.silver.ast.Label(
            name.mangled,
            invariants.toSilver().toScalaSeq(),
            position.toSilver(),
            info.toSilver(),
            trafos.toSilver()
        )
    }
}
