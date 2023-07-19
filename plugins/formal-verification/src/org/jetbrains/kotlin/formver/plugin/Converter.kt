/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.formver.plugin

import scala.jdk.javaapi.CollectionConverters
import viper.silver.ast.`NoInfo$`
import viper.silver.ast.`NoPosition$`
import viper.silver.ast.`NoTrafos$`
import viper.silver.ast.Program

private fun<T> emptySeq() = CollectionConverters.asScala(emptyList<T>()).toSeq()

class Converter {
    val program: Program
        get() = Program(
            emptySeq(),
            emptySeq(),
            emptySeq(),
            emptySeq(),
            emptySeq(),
            emptySeq(),
            `NoPosition$`.`MODULE$`,
            `NoInfo$`.`MODULE$`,
            `NoTrafos$`.`MODULE$`
        )
}