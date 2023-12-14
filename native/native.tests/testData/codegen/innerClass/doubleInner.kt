/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

open class Father(val param: String) {
    abstract inner class InClass {
        fun work(): String {
            return param
        }
    }

    inner class Child(p: String) : Father(p) {
        inner class Child2 : Father.InClass {
            constructor(): super()
        }
    }
}

fun box(): String {
    return Father("fail").Child("OK").Child2().work()
}
