package test

open class A

class MyClass(
        a: A = run {
            object O: A() {

            }

            O
        }
)

//package test
//public open class A defined in test
//public constructor A() defined in test.A
//public final class MyClass defined in test
//public constructor MyClass(a: test.A = ...) defined in test.MyClass
//value-parameter a: test.A = ... defined in test.MyClass.<init>
//local object O : test.A defined in test.MyClass.<init>.<anonymous>
//private constructor O() defined in test.MyClass.<init>.<anonymous>.O