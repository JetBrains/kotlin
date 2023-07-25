/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.scala.silicon.ast

import org.jetbrains.kotlin.formver.scala.IntoViper
import viper.silver.ast.`NoPosition$`

sealed class Position : IntoViper<viper.silver.ast.Position> {
    data object NoPosition : Position() {
        override fun toViper(): viper.silver.ast.Position = `NoPosition$`.`MODULE$`
    }

    class LineColumnPosition(private val line: Int, private val column: Int) : Position() {
        override fun toViper(): viper.silver.ast.Position = viper.silver.ast.LineColumnPosition(line, column)
    }
}
