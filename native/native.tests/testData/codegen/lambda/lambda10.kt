// OUTPUT_DATA_FILE: lambda10.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun box(): String {
    var str = "original"

    val lambda = {
        println(str)
    }

    lambda()

    str = "changed"
    lambda()

    return "OK"
}
