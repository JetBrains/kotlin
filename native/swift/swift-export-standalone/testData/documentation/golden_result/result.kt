import kotlin.native.internal.ExportedBridge

@ExportedBridge("__root___foo")
public fun __root___foo(p: Int, p2: Double): Short {
    val result = foo(p, p2)
    return result
}

