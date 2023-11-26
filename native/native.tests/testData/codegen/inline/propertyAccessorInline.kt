// OUTPUT_DATA_FILE: propertyAccessorInline.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

object C {
    const val x = 42
}

fun getC(): C {
    println(123)
    return C
}

fun box(): String {
    println(getC().x)

    return "OK"
}

