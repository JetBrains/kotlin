/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.*

abstract class Function(
    val name: MangledName,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<viper.silver.ast.Function> {
    abstract val includeInShortDump: Boolean
    abstract val formalArgs: List<Declaration.LocalVarDecl>
    abstract val retType: Type
    open val pres: List<Exp> = listOf()
    open val posts: List<Exp> = listOf()
    open val body: Exp? = null

    override fun toSilver(): viper.silver.ast.Function = viper.silver.ast.Function(
        name.mangled, formalArgs.map { it.toSilver() }.toScalaSeq(),
        retType.toSilver(), pres.toSilver().toScalaSeq(), posts.toSilver().toScalaSeq(), body.toScalaOption().toSilver(),
        pos.toSilver(), info.toSilver(), trafos.toSilver()
    )

    fun toFuncApp(
        args: List<Exp>,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ): Exp.FuncApp = Exp.FuncApp(name, args, retType, pos, info, trafos)
}

abstract class BuiltinFunction(
    name: MangledName,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos,
) : Function(name, pos, info, trafos) {
    override val includeInShortDump: Boolean = false
}