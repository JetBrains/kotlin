/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*
import kotlin.reflect.*

fun <T> bar1() = typeOf<List<T>>()
fun <T> bar2() = typeOf<List<T>>()

class D {
    fun <T> bar1() = typeOf<List<T>>()
}

fun box(): String {
    val t1 = bar1<Int>().arguments.single().type!!.classifier
    val t2 = bar2<Int>().arguments.single().type!!.classifier
    val t3 = D().bar1<Int>().arguments.single().type!!.classifier
    assertNotEquals(t1, t2)
    assertNotEquals(t1, t3)
    assertNotEquals(t2, t3)
    assertEquals(t1, bar1<Int>().arguments.single().type!!.classifier)
    assertEquals(t2, bar2<Int>().arguments.single().type!!.classifier)
    assertEquals(t3, D().bar1<Int>().arguments.single().type!!.classifier)

    return "OK"
}
