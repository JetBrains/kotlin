/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import org.jetbrains.kotlinx.dataframe.api.dataFrameOf

fun main() {
    val df = dataFrameOf("a")(1)
    println(df.a)
}