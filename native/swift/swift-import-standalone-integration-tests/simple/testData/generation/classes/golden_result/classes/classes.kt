@file:kotlin.Suppress("DEPRECATION_ERROR")

import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.NativePtr
import kotlinx.cinterop.objcPtr
import platform.darwin.NSObject

public final class Foo(private val __obj: NSObject) {
    internal fun __ptr(): NativePtr = __obj.objcPtr()
    public constructor() : this(kotlinx.cinterop.interpretObjCPointer<platform.darwin.NSObject>(classes_Foo_init())) {
    }
    public fun bar() {
        classes_Foo_bar(__ptr())
    }
    public fun baz() {
        classes_Foo_baz(__ptr())
    }
}
public final class PureSwift(private val __obj: NSObject) {
    internal fun __ptr(): NativePtr = __obj.objcPtr()
    public constructor() : this(kotlinx.cinterop.interpretObjCPointer<platform.darwin.NSObject>(classes_PureSwift_init())) {
    }
    public fun foo() {
        classes_PureSwift_foo(__ptr())
    }
}

@ImportedBridge("classes_Foo_init")
internal external fun classes_Foo_init(): kotlin.native.internal.NativePtr

@ImportedBridge("classes_Foo_bar")
internal external fun classes_Foo_bar(self: kotlin.native.internal.NativePtr): Boolean

@ImportedBridge("classes_Foo_baz")
internal external fun classes_Foo_baz(self: kotlin.native.internal.NativePtr): Boolean

@ImportedBridge("classes_PureSwift_init")
internal external fun classes_PureSwift_init(): kotlin.native.internal.NativePtr

@ImportedBridge("classes_PureSwift_foo")
internal external fun classes_PureSwift_foo(self: kotlin.native.internal.NativePtr): Boolean
