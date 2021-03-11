package test

import test.A

object <caret>A {

}

class B {
    val x = A
}

val v1 = A::class