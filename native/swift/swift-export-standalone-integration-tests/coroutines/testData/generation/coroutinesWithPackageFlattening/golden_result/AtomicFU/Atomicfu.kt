@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlinx.atomicfu.locks.SynchronizedObject::class, "22ExportedKotlinPackages7kotlinxO8atomicfuO5locksO8AtomicfuE18SynchronizedObjectC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("kotlinx_atomicfu_locks_SynchronizedObject_init_allocate")
public fun kotlinx_atomicfu_locks_SynchronizedObject_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<kotlinx.atomicfu.locks.SynchronizedObject>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlinx_atomicfu_locks_SynchronizedObject_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun kotlinx_atomicfu_locks_SynchronizedObject_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlinx.atomicfu.locks.SynchronizedObject()) }
    return run { _result; true }
}

@ExportedBridge("kotlinx_atomicfu_locks_SynchronizedObject_lock")
public fun kotlinx_atomicfu_locks_SynchronizedObject_lock(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.atomicfu.locks.SynchronizedObject
    val _result = run { __self.lock() }
    return run { _result; true }
}

@ExportedBridge("kotlinx_atomicfu_locks_SynchronizedObject_tryLock")
public fun kotlinx_atomicfu_locks_SynchronizedObject_tryLock(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.atomicfu.locks.SynchronizedObject
    val _result = run { __self.tryLock() }
    return _result
}

@ExportedBridge("kotlinx_atomicfu_locks_SynchronizedObject_unlock")
public fun kotlinx_atomicfu_locks_SynchronizedObject_unlock(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlinx.atomicfu.locks.SynchronizedObject
    val _result = run { __self.unlock() }
    return run { _result; true }
}
