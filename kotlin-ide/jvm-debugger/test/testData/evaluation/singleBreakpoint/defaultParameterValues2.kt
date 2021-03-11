package defaultParameterValues2

fun main() {
    Foo().foo()
}

interface IFoo {
    //Breakpoint!
    fun foo(a: Int = 1)
}

class Foo : IFoo {
    override fun foo(a: Int) {}
}

// EXPRESSION: Foo()
// RESULT: instance of defaultParameterValues2.Foo(id=ID): LdefaultParameterValues2/Foo;

// EXPRESSION: a
// RESULT: Parameter evaluation is not supported for '$default' methods