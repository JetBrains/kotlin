// !forceNotNullTypes: false
// !specifyLocalVariableTypeByDefault: true
package test

class Foo {
    fun execute() {
    }
}

class Bar {
    var fooNotNull = Foo()
    var fooNullable: Foo? = null
}

class Test {
    public fun test(barNotNull: Bar, barNullable: Bar?) {
        barNotNull.fooNotNull.execute()
        barNotNull.fooNullable!!.execute()
        barNullable!!.fooNotNull.execute()
        barNullable!!.fooNullable!!.execute()
    }
}