/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.IntoSilver
import org.jetbrains.kotlin.formver.viper.emptySeq
import org.jetbrains.kotlin.formver.viper.toScalaSeq
import org.jetbrains.kotlin.formver.viper.toSilver

data class Program(
    val domains: List<Domain>,
    val fields: List<Field>,
    val functions: List<Function>,
    /* no predicates */
    val methods: List<Method>,
    /* no extensions */
    val pos: Position = Position.NoPosition,
    val info: Info = Info.NoInfo,
    val trafos: Trafos = Trafos.NoTrafos,
) : IntoSilver<viper.silver.ast.Program> {
    override fun toSilver(): viper.silver.ast.Program = viper.silver.ast.Program(
        domains.toSilver().toScalaSeq(),
        fields.toSilver().toScalaSeq(),
        functions.toSilver().toScalaSeq(),
        emptySeq(), /* predicates */
        methods.toSilver().toScalaSeq(),
        emptySeq(), /* extensions */
        pos.toSilver(),
        info.toSilver(),
        trafos.toSilver(),
    )

    fun toShort(): Program = Program(
        domains.filter { it.includeInShortDump },
        fields.filter { it.includeInShortDump },
        functions.filter { it.includeInShortDump },
        methods.filter { it.includeInShortDump },
        pos,
        info,
        trafos,
    )

    fun toDebugOutput(): String = toSilver().toString()
}
