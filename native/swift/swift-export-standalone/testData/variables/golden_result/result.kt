import kotlin.native.internal.ExportedBridge

@ExportedBridge("__root___baz_get")
public fun __root___baz_get(): Int {
    return baz
}

@ExportedBridge("namespace_main_bar_get")
public fun namespace_main_bar_get(): Int {
    return namespace.main.bar
}

@ExportedBridge("namespace_main_bar_set__TypesOfArguments__int32_t__")
public fun namespace_main_bar_set(newValue: Int): Unit {
    namespace.main.bar = newValue
}

@ExportedBridge("namespace_main_foo_get")
public fun namespace_main_foo_get(): Int {
    return namespace.main.foo
}

@ExportedBridge("namespace_main_foobar__TypesOfArguments__int32_t__")
public fun namespace_main_foobar(param: Int): Int {
    val __param = param
    val _result = namespace.main.foobar(__param)
    return _result
}

