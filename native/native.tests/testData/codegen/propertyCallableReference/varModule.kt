// OUTPUT_DATA_FILE: varModule.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

var x = 42

fun box(): String {
    val p = ::x
    p.set(117)
    println(x)
    println(p.get())

    return "OK"
}
