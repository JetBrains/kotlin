@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ImportedBridge
import kotlin.native.internal.NativePtr
import kotlinx.cinterop.objcPtr
import platform.darwin.NSObject

public final class Foo(private val __obj: NSObject) {
    internal fun __ptr(): NativePtr = __obj.objcPtr()
    public constructor() : this(kotlinx.cinterop.interpretObjCPointer<platform.darwin.NSObject>(Lib_Foo_init())) {
    }
    public fun bar() {
        Lib_Foo_bar(__ptr())
    }
}

@ImportedBridge("Lib_Foo_init")
internal external fun Lib_Foo_init(): NativePtr

@ImportedBridge("Lib_Foo_bar")
internal external fun Lib_Foo_bar(self: NativePtr): Boolean
