package test

open class A

class MyClass() {
    init {
        object O: A() {

        }
    }
}

//package test
//public open class A defined in test
//public constructor A() defined in test.A
//public final class MyClass defined in test
//public constructor MyClass() defined in test.MyClass
//local object O : test.A defined in test.MyClass.<init>
//private constructor O() defined in test.MyClass.<init>.O