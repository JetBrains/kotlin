/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.IntoSilver
import scala.collection.JavaConverters
import scala.collection.immutable.Seq
import viper.silver.ast.`NoInfo$`

sealed class Info : IntoSilver<viper.silver.ast.Info> {
    internal class Wrapper(val wrappedValue: Any) : viper.silver.ast.Info {
        override fun toString(): String = "<wrapped info value>"
        override fun comment(): Seq<String> = JavaConverters.asScala(emptyList<String>()).toSeq()
        override fun isCached(): Boolean = false
    }

    companion object {
        fun fromSilver(info: viper.silver.ast.Info): Info = when (info) {
            `NoInfo$`.`MODULE$` -> NoInfo
            is Wrapper -> Wrapped(info.wrappedValue)
            else -> TODO("Unreachable")
        }
    }

    data object NoInfo : Info() {
        override fun toSilver(): viper.silver.ast.Info = `NoInfo$`.`MODULE$`
    }

    class Wrapped(val info: Any) : Info() {
        override fun toSilver(): viper.silver.ast.Info = Wrapper(info)
    }
}

@Suppress("UNCHECKED_CAST")
inline fun <I> Info.unwrapOr(orBlock: () -> I?): I? = when (this) {
    is Info.Wrapped -> info as I
    else -> orBlock()
}

val viper.silver.ast.Node.info: viper.silver.ast.Info
    get() = prettyMetadata._2()