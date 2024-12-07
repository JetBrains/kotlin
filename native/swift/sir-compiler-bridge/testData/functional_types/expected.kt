@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("root_foo_consuming_simple_closure__TypesOfArguments__function__")
public fun root_foo_consuming_simple_closure__TypesOfArguments__function__(arg1: kotlin.native.internal.NativePtr): Int {
    val __arg1 = interpretObjCPointer<function>(arg1)
    val _result = foo_consuming_simple_closure(__arg1)
    return _result
}

@ExportedBridge("root_foo_returning_closure_returning_closure__TypesOfArguments__Int32__")
public fun root_foo_returning_closure_returning_closure__TypesOfArguments__Int32__(arg1: Int): kotlin.native.internal.NativePtr {
    val __arg1 = arg1
    val _result = foo_returning_closure_returning_closure(__arg1)
    return _result.objcPtr()
}

@ExportedBridge("root_foo_returning_simple_closure__TypesOfArguments__Int32__")
public fun root_foo_returning_simple_closure__TypesOfArguments__Int32__(arg1: Int): kotlin.native.internal.NativePtr {
    val __arg1 = arg1
    val _result = foo_returning_simple_closure(__arg1)
    return _result.objcPtr()
}

@ExportedBridge("root_root_foo_consuming_closure_returning_closure__TypesOfArguments__function__")
public fun root_root_foo_consuming_closure_returning_closure__TypesOfArguments__function__(arg1: kotlin.native.internal.NativePtr): Int {
    val __arg1 = interpretObjCPointer<function>(arg1)
    val _result = root_foo_consuming_closure_returning_closure(__arg1)
    return _result
}
