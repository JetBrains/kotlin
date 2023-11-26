// OUTPUT_DATA_FILE: unit3.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun box(): String {
    foo(Unit)

    return "OK"
}

fun foo(x: Any) {
    println(x.toString())
}
