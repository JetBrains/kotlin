// OUTPUT_DATA_FILE: unchecked_cast1.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

fun box(): String {
    foo<String>("17")
    bar<String>("17")
    foo<String>(42)
    bar<String>(42)

    return "OK"
}

fun <T> foo(x: Any?) {
    val y = x as T
    println(y.toString())
}

fun <T> bar(x: Any?) {
    val y = x as? T
    println(y.toString())
}
