// OUTPUT_DATA_FILE: lambda5.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun box(): String {
    foo {
        println(it)
    }
    return "OK"
}

fun foo(f: (Int) -> Unit) {
    f(42)
}
