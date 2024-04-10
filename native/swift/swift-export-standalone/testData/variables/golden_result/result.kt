import kotlin.native.internal.ExportedBridge

@ExportedBridge("__root___baz_get")
public fun __root___baz_get(): Int {
    val _result = baz()
    return _result
}

@ExportedBridge("namespace_main_bar_get")
public fun namespace_main_bar_get(): Int {
    val _result = namespace.main.bar()
    return _result
}

@ExportedBridge("namespace_main_bar_set__TypesOfArguments__int32_t__")
public fun namespace_main_bar_set(newValue: Int): Unit {
    val __newValue = newValue
    val _result = namespace.main.bar(__newValue)
    return _result
}

@ExportedBridge("namespace_main_foo_get")
public fun namespace_main_foo_get(): Int {
    val _result = namespace.main.foo()
    return _result
}

@ExportedBridge("namespace_main_foobar__TypesOfArguments__int32_t__")
public fun namespace_main_foobar(param: Int): Int {
    val __param = param
    val _result = namespace.main.foobar(__param)
    return _result
}

