/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

class Foo {
    init {
        bar()
    }
}

inline fun bar() {
    sb.append({ "OK" }())
}

fun box(): String {
    Foo()
    return sb.toString()
}