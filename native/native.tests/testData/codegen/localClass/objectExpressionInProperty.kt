/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

abstract class Father {
    abstract inner class InClass {
        abstract fun work(): String
    }
}

class Child : Father() {
    val ChildInClass = object : Father.InClass() {
        override fun work(): String {
            return "OK"
        }
    }
}

fun box(): String {
    return Child().ChildInClass.work()
}
