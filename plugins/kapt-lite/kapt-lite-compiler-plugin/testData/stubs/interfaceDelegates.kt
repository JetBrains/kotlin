package test

interface Intf {
    val x: Int
}

class Impl(override val x: Int) : Intf

class Foo : Intf by Impl(5)