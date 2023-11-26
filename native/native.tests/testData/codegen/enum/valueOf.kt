// OUTPUT_DATA_FILE: valueOf.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

enum class E {
    E3,
    E1,
    E2
}

fun box(): String {
    println(E.valueOf("E1").toString())
    println(E.valueOf("E2").toString())
    println(E.valueOf("E3").toString())
    println(enumValueOf<E>("E1").toString())
    println(enumValueOf<E>("E2").toString())
    println(enumValueOf<E>("E3").toString())

    return "OK"
}
