/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.IntoViper

sealed class PermExp : IntoViper<viper.silver.ast.PermExp> {
    data class WildcardPerm(
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : PermExp() {
        override fun toViper(): viper.silver.ast.PermExp = viper.silver.ast.WildcardPerm(pos.toViper(), info.toViper(), trafos.toViper())
    }

    data class FullPerm(
        val pos: Position = Position.NoPosition,
        val info: Info = Info.NoInfo,
        val trafos: Trafos = Trafos.NoTrafos,
    ) : PermExp() {
        override fun toViper(): viper.silver.ast.PermExp = viper.silver.ast.FullPerm(pos.toViper(), info.toViper(), trafos.toViper())
    }
}
