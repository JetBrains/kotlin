// TODO uncomment annotations when KT-12199 will be fixed

//@file:InternalFileAnnotation1

package test2

import test1.*

internal class FromInternalClass1: InternalClass1()

//@InternalClassAnnotation
class FromClassA1 : ClassA1(10)

class FromClassB1 : ClassB1() {
    internal override val member = 10
}

fun box() {
    internalProp
    internalFun()

    InternalClass1()
    FromClassA1().member
    FromClassB1().member
    FromClassB1().func()
}
