/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

fun box(s: String): String {
    class Local {
        constructor(x: Int) {
            this.x = x
        }

        constructor(z: String) {
            x = z.length
        }

        val x: Int

        fun result() = s
    }

    return Local(42).result() + Local("zzz").result()
}

fun box(): String {
    assertEquals("OKOK", box("OK"))
    return "OK"
}