/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.IntoSilver
import org.jetbrains.kotlin.formver.viper.MangledName

open class Field(
    val name: MangledName,
    val type: Type,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<viper.silver.ast.Field> {
    open val includeInShortDump: Boolean = true
    override fun toSilver(): viper.silver.ast.Field =
        viper.silver.ast.Field(name.mangled, type.toSilver(), pos.toSilver(), info.toSilver(), trafos.toSilver())
}

class BuiltinField(
    name: MangledName,
    type: Type,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos,
) : Field(name, type, pos, info, trafos) {
    override val includeInShortDump: Boolean = false
}