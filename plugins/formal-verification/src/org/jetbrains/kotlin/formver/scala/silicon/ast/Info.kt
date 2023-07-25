/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.scala.silicon.ast

import org.jetbrains.kotlin.formver.scala.IntoViper
import viper.silver.ast.`NoInfo$`

sealed class Info : IntoViper<viper.silver.ast.Info> {
    data object NoInfo : Info() {
        override fun toViper(): viper.silver.ast.Info = `NoInfo$`.`MODULE$`
    }
}