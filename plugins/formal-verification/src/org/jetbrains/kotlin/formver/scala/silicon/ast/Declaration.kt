/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.scala.silicon.ast

import org.jetbrains.kotlin.formver.scala.IntoViper
import org.jetbrains.kotlin.formver.scala.MangledName

sealed interface Declaration : IntoViper<viper.silver.ast.Declaration> {
    data class LocalVarDecl(
        val name: MangledName,
        val type: Type,
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : Declaration {
        override fun toViper(): viper.silver.ast.LocalVarDecl =
            viper.silver.ast.LocalVarDecl(name.mangled, type.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
    }
}
