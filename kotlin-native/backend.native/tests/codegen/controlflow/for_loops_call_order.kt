/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package codegen.controlflow.for_loops_call_order

import kotlin.test.*

fun f1(): Int { print("1"); return 0 }
fun f2(): Int { print("2"); return 6 }
fun f3(): Int { print("3"); return 2 }
fun f4(): Int { print("4"); return 3 }

@Test fun runTest() {
    for (i in f1()..f2() step f3() step f4()) { }; println()
    for (i in f1() until f2() step f3() step f4()) {}; println()
    for (i in f2() downTo f1() step f3() step f4()) {}; println()
}