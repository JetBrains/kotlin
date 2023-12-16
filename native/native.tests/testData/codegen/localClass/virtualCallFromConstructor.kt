/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

abstract class WaitFor {
    init {
        condition()
    }

    abstract fun condition(): Boolean;
}

fun box(): String {
    val local = ""
    var result = "fail"
    val s = object : WaitFor() {

        override fun condition(): Boolean {
            result = "OK"
            return result.length == 2
        }
    }

    return result;
}
