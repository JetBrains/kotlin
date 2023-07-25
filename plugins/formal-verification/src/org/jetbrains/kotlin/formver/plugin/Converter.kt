/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

import org.jetbrains.kotlin.formver.scala.Option
import org.jetbrains.kotlin.formver.scala.emptySeq
import org.jetbrains.kotlin.formver.scala.seqOf
import org.jetbrains.kotlin.formver.scala.silicon.ast.*
import scala.math.BigInt
import viper.silver.ast.Program


class Converter {
    val program: Program
        get() = Program(
            emptySeq(), /* Domains */
            seqOf(
                field("foo", Type.Int),
                field("bar", Type.Ref),
                field("baz", Type.Bool),
                field("numbers", Type.Set(Type.Int))
            ), /* Fields */
            seqOf(
                function(
                    name = "sum",
                    formalArgs = emptyList(),
                    type = Type.Int,
                    pres = emptyList(),
                    posts = emptyList(),
                    body = Option.Some(Exp.Add(Exp.IntLit(BigInt.apply(40)), Exp.IntLit(BigInt.apply(2))) as Exp).toScala()
                )
            ), /* Functions */
            emptySeq(), /* Predicates */
            emptySeq(), /* Methods */
            emptySeq(), /* Extensions */
            Position.NoPosition.toViper(),
            Info.NoInfo.toViper(),
            Trafos.NoTrafos.toViper()
        )
}