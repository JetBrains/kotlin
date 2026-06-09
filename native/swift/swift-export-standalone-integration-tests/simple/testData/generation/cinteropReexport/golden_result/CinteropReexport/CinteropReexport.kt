@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("main_consumesFoo__TypesOfArguments__FooKit_Foo__")
@OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)
public fun main_consumesFoo__TypesOfArguments__FooKit_Foo__(x: kotlin.native.internal.NativePtr): Int {
    val __x = interpretObjCPointer<foo.Foo>(x)
    val _result = run { main.consumesFoo(__x) }
    return _result
}

@ExportedBridge("main_producesFoo")
@OptIn(kotlinx.cinterop.BetaInteropApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)
public fun main_producesFoo(): kotlin.native.internal.NativePtr {
    val _result = run { main.producesFoo() }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else _result.objcPtr()
}
