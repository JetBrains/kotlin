// OUTPUT_DATA_FILE: defaultArgs.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

class Z

inline fun Z.foo(x: Int = 42, y: Int = x) {
    println(y)
}

fun box(): String {
    val z = Z()
    z.foo()

    return "OK"
}
