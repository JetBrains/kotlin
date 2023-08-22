/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.scala.silicon.ast

import org.jetbrains.kotlin.formver.scala.IntoViper
import org.jetbrains.kotlin.formver.scala.MangledName
import org.jetbrains.kotlin.formver.scala.toScalaOption
import org.jetbrains.kotlin.formver.scala.toScalaSeq

open class Method(
    val name: MangledName,
    val formalArgs: List<Declaration.LocalVarDecl>,
    val formalReturns: List<Declaration.LocalVarDecl>,
    val pres: List<Exp>,
    val posts: List<Exp>,
    val body: Stmt.Seqn?,
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoViper<viper.silver.ast.Method> {
    open val includeInShortDump: Boolean = true
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

}

class BuiltInMethod(
    name: MangledName,
    formalArgs: List<Declaration.LocalVarDecl>,
    formalReturns: List<Declaration.LocalVarDecl>,
    pres: List<Exp>,
    posts: List<Exp>,
    body: Stmt.Seqn?,
    pos: Position = Position.NoPosition,
    info: Info = Info.NoInfo,
    trafos: Trafos = Trafos.NoTrafos,
) : Method(name, formalArgs, formalReturns, pres, posts, body, pos, info, trafos) {
    override val includeInShortDump: Boolean = false
}
