@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(oh.my.state.ExtractedByTypealias::class, "22ExportedKotlinPackages2ohO2myO5stateO5stateE20ExtractedByTypealiasC")
@file:kotlin.native.internal.objc.BindClassToObjCName(oh.my.state.State::class, "22ExportedKotlinPackages2ohO2myO5stateO5stateE5StateC")
@file:kotlin.native.internal.objc.BindClassToObjCName(oh.my.state.inner.InnerState::class, "22ExportedKotlinPackages2ohO2myO5stateO5innerO5stateE10InnerStateC")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("oh_my_state_ExtractedByTypealias_init_allocate")
public fun oh_my_state_ExtractedByTypealias_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<oh.my.state.ExtractedByTypealias>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("oh_my_state_ExtractedByTypealias_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun oh_my_state_ExtractedByTypealias_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, oh.my.state.ExtractedByTypealias())
}

@ExportedBridge("oh_my_state_State_init_allocate")
public fun oh_my_state_State_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<oh.my.state.State>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("oh_my_state_State_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_oh_my_state_inner_InnerState___")
public fun oh_my_state_State_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_oh_my_state_inner_InnerState___(__kt: kotlin.native.internal.NativePtr, innerState: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __innerState = if (innerState == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(innerState) as oh.my.state.inner.InnerState
    kotlin.native.internal.initInstance(____kt, oh.my.state.State(__innerState))
}

@ExportedBridge("oh_my_state_State_innerState_get")
public fun oh_my_state_State_innerState_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as oh.my.state.State
    val _result = __self.innerState
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("oh_my_state_inner_InnerState_bytes_get")
public fun oh_my_state_inner_InnerState_bytes_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as oh.my.state.inner.InnerState
    val _result = __self.bytes
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("oh_my_state_inner_InnerState_init_allocate")
public fun oh_my_state_inner_InnerState_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<oh.my.state.inner.InnerState>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("oh_my_state_inner_InnerState_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_ByteArray___")
public fun oh_my_state_inner_InnerState_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_ByteArray___(__kt: kotlin.native.internal.NativePtr, bytes: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __bytes = if (bytes == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(bytes) as kotlin.ByteArray
    kotlin.native.internal.initInstance(____kt, oh.my.state.inner.InnerState(__bytes))
}
