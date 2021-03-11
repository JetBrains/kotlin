@file:InternalFileAnnotation1

package test2

import test1.*

internal class FromInternalClass1: InternalClass1()

@InternalClassAnnotation1
class FromClassA1 : ClassA1(10) {
    @InternalClassAnnotation1
    class Nested {
        @InternalFunctionAnnotation1
        fun foo() {}
    }
}

class FromClassB1 : ClassB1() {
    internal override val member = 10
}

@InternalFunctionAnnotation1
fun foo() {}

fun box() {
    internalProp
    internalFun()

    InternalClass1()
    FromClassA1().member
    FromClassB1().member
    FromClassB1().func()
}
