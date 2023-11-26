// OUTPUT_DATA_FILE: secondaryConstructor.out
/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */


import kotlin.test.*

class Outer(val x: Int) {
    inner class Inner() {
        inner class InnerInner() {

            init {
                println(x)
            }

            lateinit var s: String

            constructor(s: String) : this() {
                this.s = s
            }
        }
    }
}

fun box(): String {
    Outer(42).Inner().InnerInner("zzz")

    return "OK"
}
