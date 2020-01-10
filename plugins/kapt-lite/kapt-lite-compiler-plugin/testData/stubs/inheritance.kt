package inheritance

interface Intf
abstract class Base

class Foo : Base(), Intf

//

interface A1 {
    fun foo()
}

interface A2 {
    fun bar()
}

class A1A2 : A1, A2 {
    override fun foo() {}
    override fun bar() {}
}

//

interface B1 {
    fun foo() {}
}

interface B2 {
    fun foo() {}
}

class B1B2 : B1, B2 {
    override fun foo() {}
}

//

abstract class C1
abstract class C2 : C1()
abstract class C3 : C1()
class C4 : C3(), Intf, A1, A2, B1, B2 {
    override fun foo() {}
    override fun bar() {}
}