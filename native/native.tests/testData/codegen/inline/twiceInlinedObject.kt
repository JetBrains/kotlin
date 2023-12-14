/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

inline fun exec(f: () -> Unit) = f()

inline fun test2() {
    val obj = object {
        fun sayOk() = sb.append("OK")
    }
    obj.sayOk()
}

inline fun noExec(f: () -> Unit) { }

fun box(): String {
    exec {
        test2()
    }
    noExec {
        test2()
    }

    return sb.toString()
}