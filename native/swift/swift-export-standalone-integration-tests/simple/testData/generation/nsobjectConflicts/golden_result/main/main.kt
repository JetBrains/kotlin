@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(ClassA::class, "4main6ClassAC")
@file:kotlin.native.internal.objc.BindClassToObjCName(ClassB::class, "4main6ClassBC")
@file:kotlin.native.internal.objc.BindClassToObjCName(Semaphore::class, "_main_Semaphore")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("ClassB_hash__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__")
public fun ClassB_hash__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(self: kotlin.native.internal.NativePtr, intoOk: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ClassB
    val __intoOk = kotlin.native.internal.ref.dereferenceExternalRCRef(intoOk) as kotlin.Any
    __self.hash(__intoOk)
    return true
}

@ExportedBridge("ClassB_mutableCopy")
public fun ClassB_mutableCopy(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as ClassB
    val _result = __self.mutableCopy()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ClassA_init_allocate")
public fun __root___ClassA_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<ClassA>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___ClassA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, ClassA())
    return true
}

@ExportedBridge("__root___ClassB_init_allocate")
public fun __root___ClassB_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<ClassB>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___ClassB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun __root___ClassB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, ClassB())
    return true
}

@ExportedBridge("__root___forwardingTarget__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__")
public fun __root___forwardingTarget__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(`for`: kotlin.native.internal.NativePtr): Boolean {
    val __for = kotlin.native.internal.ref.dereferenceExternalRCRef(`for`) as kotlin.Any
    forwardingTarget(__for)
    return true
}

@ExportedBridge("__root___hash_get")
public fun __root___hash_get(): Int {
    val _result = hash
    return _result
}
