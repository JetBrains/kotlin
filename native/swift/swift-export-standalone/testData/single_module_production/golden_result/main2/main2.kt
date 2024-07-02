import kotlin.native.internal.ExportedBridge

@ExportedBridge("demo_shared_foo2")
public fun demo_shared_foo2(): Int {
    val _result = demo.shared.foo2()
    return _result
}
