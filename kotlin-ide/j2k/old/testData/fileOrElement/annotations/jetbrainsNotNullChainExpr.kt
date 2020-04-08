// !FORCE_NOT_NULL_TYPES: false
// !SPECIFY_LOCAL_VARIABLE_TYPE_BY_DEFAULT: true
package test

internal class Foo {
    fun execute() {}
}

internal class Bar {
    var fooNotNull = Foo()
    var fooNullable: Foo? = null
}

internal class Test {
    fun test(barNotNull: Bar, barNullable: Bar) {
        barNotNull.fooNotNull.execute()
        barNotNull.fooNullable!!.execute()
        barNullable.fooNotNull.execute()
        barNullable.fooNullable!!.execute()
    }
}