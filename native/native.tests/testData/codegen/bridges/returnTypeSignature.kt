/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

import kotlin.test.*

class A { }

class B { }

class C { }

interface Parser<in IN: Any, out OUT: Any> {
    fun parse(source: IN): OUT
}

interface MultiParser<in IN: Any, out OUT: Any> {
    fun parse(source: IN): Collection<OUT>
}

interface ExtendsInterface<T: Any>: Parser<A, T>, MultiParser<B, T> {
    override fun parse(source: B): Collection<T> = ArrayList<T>()
}

abstract class AbstractClass(): ExtendsInterface<C> {
    public override fun parse(source: A): C = C()
}

fun box(): String {
    val array = object : AbstractClass() { }.parse(B())

    return "OK"
}