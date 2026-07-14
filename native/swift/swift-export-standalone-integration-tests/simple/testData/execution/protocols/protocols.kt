// KIND: STANDALONE
// MODULE: Main
// FILE: protocols.kt

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

interface Foo {
    fun identity(obj: Foo): Foo
    var property: Foo
}

class SomeFoo: Foo {
    companion object {
        var instance = SomeFoo()
    }

    override fun identity(obj: Foo): Foo {
        assert(obj is SomeFoo)
        return obj
    }

    private var _property: Foo = SomeFoo.instance
    override var property: Foo
        get() = _property
        set(newValue) {
            assert(newValue is SomeFoo)
            _property = newValue
        }
}

fun identity(obj: Foo): Foo {
    assert(obj is SomeFoo)
    return obj
}

private var _property: Foo = SomeFoo()
var property: Foo
    get() = _property
    set(newValue) {
        assert(newValue is SomeFoo)
        _property = newValue
    }

fun nullableIdentity(value: Foo?): Foo? = value
var nullableProperty: Foo? = null

fun listIdentity(value: List<Foo>): List<Foo> = value
var listProperty: List<Foo> = emptyList()

fun nullablesListIdentity(value: List<Foo?>): List<Foo?> = value
var nullablesListProperty: List<Foo?> = emptyList()

// FILE: protocolMembers.kt

@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class)

interface Bar {
    fun identity(obj: Foo): Foo
    var property: Foo
}

class SomeBar: Bar {
    override fun identity(obj: Foo): Foo {
        assert(obj is SomeFoo)
        return obj
    }

    private var _property: Foo = SomeFoo()

    override var property: Foo
        get() = _property
        set(newValue) {
            assert(newValue is SomeFoo)
            _property = newValue
        }
}

// FILE: existentials.kt

interface Baz {
    var value: Baz
    fun identity(baz: Baz): Baz = baz
}

private class SomeBaz : Baz {
    override var value: Baz = this
}

var value: Baz = SomeBaz()

fun identity(baz: Baz): Baz = baz

// FILE: functional_interface.kt

fun interface FunctionalInterface {
    fun getNumber(): Int
}

fun testFunctionalInterface(arg: FunctionalInterface): Int = arg.getNumber()

// FILE: existentialInheritance.kt

interface InhFoo

open class InhBar

internal class InhBaz : InhBar(), InhFoo

fun returnInhFoo(): InhFoo = InhBaz()

interface InhIface {
    fun ping(): Int
}

open class InhBarWithIface : InhIface {
    override fun ping(): Int = 42
}

internal class InhBaz2 : InhBarWithIface()

fun returnInhIface(): InhIface = InhBaz2()

// FILE: myinterfacea.kt

package a

interface MyInterface {
    fun foo(): String
}

// FILE: myinterfaceb.kt

package b

interface MyInterface {
    fun bar(): String
}

// FILE: myclass.kt

open class MyClassA : a.MyInterface {
    override fun foo() = "A"
}

internal class MyClassB : MyClassA(), b.MyInterface {
    override fun bar() = "B"
}

fun returnMyClassBAsMyInterfaceA(): a.MyInterface = MyClassB()

fun returnMyClassBAsMyInterfaceB(): b.MyInterface = MyClassB()
