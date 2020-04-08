// !DIAGNOSTICS: -DUPLICATE_CLASS_NAMES
val <error>a</error> : Int = 1
val <error>a</error> : Int = 1

<error>fun foo()</error> {}
<error>fun foo()</error> {}

enum class EnumClass {
    <error>FOO</error>,
    <error>FOO</error>
}

class A {
    val <error>a</error> : Int = 1
    val <error>a</error> : Int = 1

    <error>fun foo()</error> {}
    <error>fun foo()</error> {}
}

object B {
    <error>class <error>C</error></error>
    <error>class <error>C</error></error>
}

fun <<error>T</error>, <error>T</error>> PairParam() {}
class PParam<<error>T</error>, <error>T</error>> {}

val <<error>T</error>, <error>T</error>> T.fooParam : Int get() = 1
