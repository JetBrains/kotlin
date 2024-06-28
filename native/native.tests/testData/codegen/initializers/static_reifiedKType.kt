/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.*
import kotlin.reflect.*

class R<T, U, V, X>
interface S

fun box(): String {
    inline fun <reified T, U, V> kTypeOf() where V : List<Int>, V : S, U : T = typeOf<R<T, in U, out V, *>>()

    class XX(val x:List<Int>) : List<Int> by x, S

    val type = kTypeOf<List<Int>, ArrayList<Int>, XX>()
    assertEquals("R<kotlin.collections.List<kotlin.Int>, in U, out V, *>", type.toString())
    assertEquals("[T]", (type.arguments[1].type!!.classifier as KTypeParameter).upperBounds.toString())
    assertEquals("[kotlin.collections.List<kotlin.Int>, S]",
            (type.arguments[2].type!!.classifier as KTypeParameter).upperBounds.toString())

    return "OK"
}
