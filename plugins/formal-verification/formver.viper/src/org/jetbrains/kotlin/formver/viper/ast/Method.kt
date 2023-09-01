/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.IntoViper
import org.jetbrains.kotlin.formver.viper.MangledName
import org.jetbrains.kotlin.formver.viper.toScalaOption
import org.jetbrains.kotlin.formver.viper.toScalaSeq

abstract class Method(
    val name: MangledName,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoViper<viper.silver.ast.Method> {
    open val includeInShortDump: Boolean = true
    abstract val formalArgs: List<Declaration.LocalVarDecl>
    abstract val formalReturns: List<Declaration.LocalVarDecl>
    open val pres: List<Exp> = listOf()
    open val posts: List<Exp> = listOf()
    open val body: Stmt.Seqn? = null

    override fun toViper(): viper.silver.ast.Method =
        viper.silver.ast.Method(
            name.mangled,
            formalArgs.map { it.toViper() }.toScalaSeq(),
            formalReturns.map { it.toViper() }.toScalaSeq(),
            pres.map { it.toViper() }.toScalaSeq(),
            posts.map { it.toViper() }.toScalaSeq(),
            body.toScalaOption().map { it.toViper() },
            pos.toViper(),
            info.toViper(),
            trafos.toViper()
        )

    fun toMethodCall(
        args: List<Exp>,
        targets: List<Exp.LocalVar>,
        pos: Position = Position.NoPosition,
        info: Info = Info.NoInfo,
        trafos: Trafos = Trafos.NoTrafos,
    ) = Stmt.MethodCall(name, args, targets, pos, info, trafos)
}

class UserMethod(
    name: MangledName,
    override val formalArgs: List<Declaration.LocalVarDecl>,
    returnVar: Declaration.LocalVarDecl,
    override val pres: List<Exp>,
    override val posts: List<Exp>,
    override val body: Stmt.Seqn?,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos,
) : Method(name, pos, info, trafos) {
    override val formalReturns: List<Declaration.LocalVarDecl> = listOf(returnVar)
}

abstract class BuiltInMethod(
    name: MangledName,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos,
) : Method(name, pos, info, trafos) {
    override val includeInShortDump: Boolean = false
}
