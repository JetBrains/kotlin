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