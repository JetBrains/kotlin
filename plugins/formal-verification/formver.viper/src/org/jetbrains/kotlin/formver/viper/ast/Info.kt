/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.viper.ast

import org.jetbrains.kotlin.formver.viper.IntoSilver
import viper.silver.ast.`NoInfo$`

sealed class Info : IntoSilver<viper.silver.ast.Info> {
    data object NoInfo : Info() {
        override fun toSilver(): viper.silver.ast.Info = `NoInfo$`.`MODULE$`
    }
}