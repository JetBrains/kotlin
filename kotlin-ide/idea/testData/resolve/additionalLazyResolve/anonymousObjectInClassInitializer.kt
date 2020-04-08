package test

open class A

class MyClass() {
    init {
        val a = object: A() {

        }
    }
}

//package test
//public open class A defined in test
//public constructor A() defined in test.A
//public final class MyClass defined in test
//public constructor MyClass() defined in test.MyClass
//val a: test.MyClass.<init>.<no name provided> defined in test.MyClass.<init>
//local final class <no name provided> : test.A defined in test.MyClass.<init>
//public constructor <no name provided>() defined in test.MyClass.<init>.<no name provided>