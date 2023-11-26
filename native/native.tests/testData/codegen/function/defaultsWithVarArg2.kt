// OUTPUT_DATA_FILE: defaultsWithVarArg2.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun foo(vararg arr: Int = intArrayOf(1, 2)) {
    arr.forEach { println(it) }
}

fun box(): String {
    foo()
    foo(42)

    return "OK"
}
