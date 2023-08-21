/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.scala.silicon.ast

import org.jetbrains.kotlin.formver.scala.IntoViper
import org.jetbrains.kotlin.formver.scala.MangledName

open class Field(
    val name: MangledName,
    val type: Type,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoViper<viper.silver.ast.Field> {
    open val includeInShortDump: Boolean = true
    override fun toViper(): viper.silver.ast.Field =
        viper.silver.ast.Field(name.mangled, type.toViper(), pos.toViper(), info.toViper(), trafos.toViper())
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