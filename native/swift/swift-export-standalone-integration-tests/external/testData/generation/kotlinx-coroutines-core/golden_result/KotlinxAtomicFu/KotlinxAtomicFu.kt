@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlinx.atomicfu.locks.SynchronizedObject::class, "22ExportedKotlinPackages7kotlinxO8atomicfuO5locksO15KotlinxAtomicFuE18SynchronizedObjectC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*

@ExportedBridge("kotlinx_atomicfu_locks_SynchronizedObject_init_allocate")
public fun kotlinx_atomicfu_locks_SynchronizedObject_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<kotlinx.atomicfu.locks.SynchronizedObject>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlinx_atomicfu_locks_SynchronizedObject_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun kotlinx_atomicfu_locks_SynchronizedObject_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, kotlinx.atomicfu.locks.SynchronizedObject())
    return true
}

@ExportedBridge("kotlinx_atomicfu_locks_SynchronizedObject_lock")
public fun kotlinx_atomicfu_locks_SynchronizedObject_lock(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.atomicfu.locks.SynchronizedObject
    __self.lock()
    return true
}

@ExportedBridge("kotlinx_atomicfu_locks_SynchronizedObject_tryLock")
public fun kotlinx_atomicfu_locks_SynchronizedObject_tryLock(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.atomicfu.locks.SynchronizedObject
    val _result = __self.tryLock()
    return _result
}

@ExportedBridge("kotlinx_atomicfu_locks_SynchronizedObject_unlock")
public fun kotlinx_atomicfu_locks_SynchronizedObject_unlock(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.atomicfu.locks.SynchronizedObject
    __self.unlock()
    return true
}
