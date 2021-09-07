@file:InternalFileAnnotation

package test

import test.InternalClass1

// InternalClass1, ClassA1, ClassB1 are in module1
class ClassInheritedFromInternal1: InternalClass1()

@InternalClassAnnotation
class ClassAA1 : ClassA1(10)

class ClassBB1 : ClassB1() {
    internal override val member = 10
}

// InternalClass2, ClassA2, ClassB2 are in module2
class ClassInheritedFromInternal2: InternalClass2()

class ClassAA2 : ClassA2(10)

class ClassBB2 : ClassB2() {
    internal override val member = 10
}

fun f() {
    val x1 = ClassAA1().member
    val x2 = ClassAA2().member
}


