/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package kt57791

abstract class Ckt57791 {
    abstract fun baz(): Int
}

object Okt57791 : Ckt57791() {
    override fun baz() = 117
}

class Ckt57791Final : Ckt57791() {
    override fun baz() = 42
}

interface Foo {
    fun getCkt57791(): Ckt57791Final
}

fun foobar(f: Boolean, foo: Foo): Boolean {
    val z = if (f) Okt57791 else foo.getCkt57791()
    return z.baz() == 42
}