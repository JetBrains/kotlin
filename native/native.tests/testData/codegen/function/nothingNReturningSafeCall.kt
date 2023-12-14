/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

import kotlin.test.*

fun Any.nothing(): Nothing {
    while (true) {}
}

class Anything

fun testFunction1(obj: Any?): Nothing? = obj?.nothing()
fun testFunction2(obj: Any?): Any? = obj?.nothing()
fun testFunction3(obj: Any?): String? = obj?.nothing()
fun testFunction4(obj: Any?): Unit? = obj?.nothing()
fun testFunction5(obj: Any?): Anything? = obj?.nothing()

fun testLambda1() {
    val block: (Any?) -> Nothing? = {
        it?.nothing()
    }
    block(null)
}

fun testLambda2() {
    val block: (Any?) -> Nothing? = {
        println() // more than one statement inside of the body
        it?.nothing()
    }
    block(null)
}

fun box(): String {
    testFunction1(null)
    testFunction2(null)
    testFunction3(null)
    testFunction4(null)
    testFunction5(null)

    testLambda1()
    testLambda2()

    return "OK"
}
