/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.IntoSilver
import viper.silver.ast.`NoPosition$`

sealed class Position : IntoSilver<viper.silver.ast.Position> {

    internal class Wrapper<P>(val wrappedValue: P) : viper.silver.ast.Position {
        override fun toString(): String = "<wrapped value>"
    }

    companion object {
        fun fromSilver(pos: viper.silver.ast.Position): Position = when (pos) {
            `NoPosition$`.`MODULE$` -> NoPosition
            is Wrapper<*> -> Wrapped(pos.wrappedValue)
            else -> TODO("Unreachable")
        }
    }

    data object NoPosition : Position() {
        override fun toSilver(): viper.silver.ast.Position = `NoPosition$`.`MODULE$`
    }

    class Wrapped<P>(val source: P) : Position() {
        override fun toSilver(): viper.silver.ast.Position = Wrapper(source)
    }
}

//region Define Extension Function Utilities
inline fun <reified P> Position.unwrapOr(orBlock: () -> P?): P? = when (this) {
    is Position.Wrapped<*> -> source as? P
    else -> orBlock()
}

fun Position.isEmpty(): Boolean = (this is Position.NoPosition)
//endregion