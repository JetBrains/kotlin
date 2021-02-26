/*
 * Copyright 2010-2020 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import a.*

fun main() {
    println(fold(0, intArrayOf(1, 2, 3)) { x, y -> x + y })
}