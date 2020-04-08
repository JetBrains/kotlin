package second

import third.B
import fourth.X.Y
import third.D as D_
import fourth.*
import third.B.*

class A

class <caret>Test {
    val a = A()
    val b = B()
    val d_ = D_()
    val c = C()
    val x = X()
    val y = Y()
}