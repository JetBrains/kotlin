// OUTPUT_DATA_FILE: localFunctionInInitializerBlock.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

class Foo {
    init {
        bar()
    }
}

inline fun bar() {
    println({ "OK" }())
}

fun box(): String {
    Foo()

    return "OK"
}
