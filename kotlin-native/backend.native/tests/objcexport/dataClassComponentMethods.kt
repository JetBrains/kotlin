/*
 * Copyright 2010-2022 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package dataClassComponentMethods

data class DataClassWithComponentMethods(val x: Int, val y: Int)

class RegularClassWithComponentMethods {
    fun component1() = 3
    fun component3() = 4
}

fun component1() = 5
fun component4() = 6
