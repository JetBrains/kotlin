import kotlin.native.internal.ExportedBridge

@ExportedBridge("foo_bar_foo")
public fun foo_bar_foo(): Unit {
    val _result = foo.bar.foo()
    return _result
}

