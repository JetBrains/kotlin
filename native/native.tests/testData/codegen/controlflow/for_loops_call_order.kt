/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

val sb = StringBuilder()

fun f1(): Int { sb.append(1); return 0 }
fun f2(): Int { sb.append(2); return 6 }
fun f3(): Int { sb.append(3); return 2 }
fun f4(): Int { sb.append(4); return 3 }

fun box(): String {
    for (i in f1()..f2() step f3() step f4()) { }; sb.appendLine()
    for (i in f1() until f2() step f3() step f4()) {}; sb.appendLine()
    for (i in f2() downTo f1() step f3() step f4()) {}; sb.appendLine()

    assertEquals("""
        1234
        1234
        2134

    """.trimIndent(), sb.toString())
    return "OK"
}