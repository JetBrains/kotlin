// OUTPUT_DATA_FILE: boxing14.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun box(): String {
    42.println()
    val nonConst = 42
    nonConst.println()

    return "OK"
}

fun <T> T.println() = println(this.toString())
