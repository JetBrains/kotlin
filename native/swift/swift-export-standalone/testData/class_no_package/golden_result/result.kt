import kotlin.native.internal.ExportedBridge

@ExportedBridge("__root___bar__TypesOfArguments__voidSTAR__")
public fun __root___bar(p: Any): Any {
    val result = bar(p)
    return result
}

@ExportedBridge("__root___foo_get")
public fun __root___foo_get(): Any {
    val result = foo()
    return result
}

@ExportedBridge("__root___foo_set__TypesOfArguments__voidSTAR__")
public fun __root___foo_set(newValue: Any): Unit {
    val result = foo(newValue)
    return result
}

