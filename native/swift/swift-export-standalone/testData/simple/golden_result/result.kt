import kotlin.native.internal.ExportedBridge

@ExportedBridge("namespace1_main_foobar")
public fun namespace1_main_foobar(param: Int): Int {
    val result = namespace1.main.foobar(param)
    return result
}

@ExportedBridge("namespace1_bar")
public fun namespace1_bar(): Int {
    val result = namespace1.bar()
    return result
}

@ExportedBridge("namespace2_foo")
public fun namespace2_foo(): Int {
    val result = namespace2.foo()
    return result
}

@ExportedBridge("__root___foo")
public fun __root___foo(): Int {
    val result = foo()
    return result
}
