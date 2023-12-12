import kotlin.native.internal.ExportedBridge

@ExportedBridge("namespace1_main_foobar")
public fun namespace1_main_foobar(): Int {
    val result = namespace1.main.foobar()
    return result
}

@ExportedBridge("namespace1_foo")
public fun namespace1_foo(): Int {
    val result = namespace1.foo()
    return result
}

@ExportedBridge("namespace2_bar")
public fun namespace2_bar(): Int {
    val result = namespace2.bar()
    return result
}
