/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.scala.silicon.ast

import org.jetbrains.kotlin.formver.scala.IntoViper
import org.jetbrains.kotlin.formver.scala.emptySeq
import org.jetbrains.kotlin.formver.scala.toScalaSeq
import org.jetbrains.kotlin.formver.scala.toViper

data class Program(
    val domains: List<Domain>,
    val fields: List<Field>,
    /* no functions */
    /* no predicates */
    val methods: List<Method>,
    /* no extensions */
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoViper<viper.silver.ast.Program> {
    override fun toViper(): viper.silver.ast.Program = viper.silver.ast.Program(
        domains.toViper().toScalaSeq(),
        fields.toViper().toScalaSeq(),
        emptySeq(), /* functions */
        emptySeq(), /* predicates */
        methods.toViper().toScalaSeq(),
        emptySeq(), /* extensions */
        pos.toViper(),
        info.toViper(),
        trafos.toViper(),
    )
}
