/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import a.*

fun main(args: Array<String>) {
    println(A.Z1.x)
    println(A.valueOf("Z2").x)
    println(A.values()[2].x)
}