// !forceNotNullTypes: false
// !specifyLocalVariableTypeByDefault: true
package test

internal class Foo {
    internal fun execute() {
    }
}

internal class Bar {
    internal var fooNotNull = Foo()
    internal var fooNullable: Foo? = null
}

internal class Test {
    fun test(barNotNull: Bar, barNullable: Bar?) {
        barNotNull.fooNotNull.execute()
        barNotNull.fooNullable!!.execute()
        barNullable!!.fooNotNull.execute()
        barNullable.fooNullable!!.execute()
    }
}