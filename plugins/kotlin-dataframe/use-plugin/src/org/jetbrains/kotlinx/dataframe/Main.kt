/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlinx.dataframe

import org.jetbrains.kotlinx.dataframe.annotations.DataSchema
import org.jetbrains.kotlinx.dataframe.api.dataFrameOf

@DataSchema
interface Schema {
    val i: Int
}

fun main() {
    val df = dataFrameOf("i")(1, 2, 3)/*.cast<Schema>()*/
//    df.i
}