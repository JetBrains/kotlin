/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the LICENSE file.
 */

package foo.bar

class R<T> {
    inline fun bar(t: T) {
        println("just a single class: $t")
    }
}

class C {
    inline fun foo() {
        println("first level")
    }

    class D {
        inline fun foo() {
            println("second level")
        }

        class E {
            inline fun foo() {
                println("third levelxz")
            }
        }
    }
}

class C2 {
    inline fun foo() {
        println("inner first level")
    }

    inner class D2 {
        inline fun foo() {
            println("inner second level")
        }

        inner class E2 {
            inline fun foo() {
                println("inner third level")
            }
        }
    }
}

class C3<X> {
    inline fun foo(x: X) {
        println("types first level: $x")
    }

    class D3<X> {
        inline fun foo(x: X) {
            println("types second level $x")
        }

        class E3<X> {
            inline fun foo(x: X) {
                println("types third level $x")
            }
        }
    }
}

class C4<X> {
    inline fun foo(x: X) {
        println("inner types first level: $x")
    }

    inner class D4<Y> {
        inline fun foo(x: X, y: Y) {
            println("inner types second level $x, $y")
        }

        inner class E4<Z> {
            inline fun foo(x: X, y: Y, z: Z) {
                println("inner types third level $x, $y, $z")
            }
        }
    }
}

