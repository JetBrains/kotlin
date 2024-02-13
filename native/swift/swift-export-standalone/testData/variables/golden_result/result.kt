import kotlin.native.internal.ExportedBridge

@ExportedBridge("namespace_main_foo_get")
public fun namespace_main_foo_get(): Int {
    val result = namespace.main.foo()
    return result
}

@ExportedBridge("namespace_main_bar_get")
public fun namespace_main_bar_get(): Int {
    val result = namespace.main.bar()
    return result
}

@ExportedBridge("namespace_main_bar_set__TypesOfArguments__int32_t__")
public fun namespace_main_bar_set(newValue: Int): Unit {
    val result = namespace.main.bar(newValue)
    return result
}

@ExportedBridge("namespace_main_foobar__TypesOfArguments__int32_t__")
public fun namespace_main_foobar(param: Int): Int {
    val result = namespace.main.foobar(param)
    return result
}

@ExportedBridge("__root___baz")
public fun __root___baz(): Int {
    val result = baz()
    return result
}

