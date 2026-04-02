@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Array::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE5ArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Exception::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE9ExceptionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.IllegalStateException::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE21IllegalStateExceptionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.RuntimeException::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE16RuntimeExceptionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Throwable::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE9ThrowableC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Iterator::class, "_Iterator")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.coroutines.CoroutineContext::class, "_CoroutineContext")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.coroutines.cancellation.CancellationException::class, "22ExportedKotlinPackages6kotlinO10coroutinesO12cancellationO12KotlinStdlibE21CancellationExceptionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.coroutines.CoroutineContext.Element::class, "__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Element")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.coroutines.CoroutineContext.Key::class, "__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("kotlin_Array_get__TypesOfArguments__Swift_Int32__")
public fun kotlin_Array_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Array<kotlin.Any?>
    val __index = index
    val _result = run { __self.`get`(__index) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_Array_iterator")
public fun kotlin_Array_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Array<kotlin.Any?>
    val _result = run { __self.iterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_Array_set__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_Array_set__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, index: Int, value: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Array<kotlin.Any?>
    val __index = index
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Any
    val _result = run { __self.`set`(__index, __value) }
    return run { _result; true }
}

@ExportedBridge("kotlin_Array_size_get")
public fun kotlin_Array_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Array<kotlin.Any?>
    val _result = run { __self.size }
    return _result
}

@ExportedBridge("kotlin_Exception_init_allocate")
public fun kotlin_Exception_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<kotlin.Exception>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.Exception()) }
    return run { _result; true }
}

@ExportedBridge("kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___")
public fun kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt: kotlin.native.internal.NativePtr, message: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __message = if (message == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.String>(message)
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.Exception(__message)) }
    return run { _result; true }
}

@ExportedBridge("kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___")
public fun kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt: kotlin.native.internal.NativePtr, message: kotlin.native.internal.NativePtr, cause: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __message = if (message == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.String>(message)
    val __cause = if (cause == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(cause) as kotlin.Throwable
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.Exception(__message, __cause)) }
    return run { _result; true }
}

@ExportedBridge("kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___")
public fun kotlin_Exception_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt: kotlin.native.internal.NativePtr, cause: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __cause = if (cause == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(cause) as kotlin.Throwable
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.Exception(__cause)) }
    return run { _result; true }
}

@ExportedBridge("kotlin_IllegalStateException_init_allocate")
public fun kotlin_IllegalStateException_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<kotlin.IllegalStateException>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_IllegalStateException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun kotlin_IllegalStateException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.IllegalStateException()) }
    return run { _result; true }
}

@ExportedBridge("kotlin_IllegalStateException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___")
public fun kotlin_IllegalStateException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt: kotlin.native.internal.NativePtr, message: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __message = if (message == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.String>(message)
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.IllegalStateException(__message)) }
    return run { _result; true }
}

@ExportedBridge("kotlin_IllegalStateException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___")
public fun kotlin_IllegalStateException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt: kotlin.native.internal.NativePtr, message: kotlin.native.internal.NativePtr, cause: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __message = if (message == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.String>(message)
    val __cause = if (cause == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(cause) as kotlin.Throwable
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.IllegalStateException(__message, __cause)) }
    return run { _result; true }
}

@ExportedBridge("kotlin_IllegalStateException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___")
public fun kotlin_IllegalStateException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt: kotlin.native.internal.NativePtr, cause: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __cause = if (cause == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(cause) as kotlin.Throwable
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.IllegalStateException(__cause)) }
    return run { _result; true }
}

@ExportedBridge("kotlin_RuntimeException_init_allocate")
public fun kotlin_RuntimeException_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<kotlin.RuntimeException>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.RuntimeException()) }
    return run { _result; true }
}

@ExportedBridge("kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___")
public fun kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt: kotlin.native.internal.NativePtr, message: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __message = if (message == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.String>(message)
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.RuntimeException(__message)) }
    return run { _result; true }
}

@ExportedBridge("kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___")
public fun kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt: kotlin.native.internal.NativePtr, message: kotlin.native.internal.NativePtr, cause: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __message = if (message == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.String>(message)
    val __cause = if (cause == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(cause) as kotlin.Throwable
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.RuntimeException(__message, __cause)) }
    return run { _result; true }
}

@ExportedBridge("kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___")
public fun kotlin_RuntimeException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt: kotlin.native.internal.NativePtr, cause: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __cause = if (cause == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(cause) as kotlin.Throwable
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.RuntimeException(__cause)) }
    return run { _result; true }
}

@ExportedBridge("kotlin_Throwable_cause_get")
public fun kotlin_Throwable_cause_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Throwable
    val _result = run { __self.cause }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_Throwable_getStackTrace")
public fun kotlin_Throwable_getStackTrace(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Throwable
    val _result = run { __self.getStackTrace() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_Throwable_init_allocate")
public fun kotlin_Throwable_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<kotlin.Throwable>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___")
public fun kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt: kotlin.native.internal.NativePtr, message: kotlin.native.internal.NativePtr, cause: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __message = if (message == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.String>(message)
    val __cause = if (cause == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(cause) as kotlin.Throwable
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.Throwable(__message, __cause)) }
    return run { _result; true }
}

@ExportedBridge("kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___")
public fun kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt: kotlin.native.internal.NativePtr, message: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __message = if (message == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.String>(message)
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.Throwable(__message)) }
    return run { _result; true }
}

@ExportedBridge("kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___")
public fun kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt: kotlin.native.internal.NativePtr, cause: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __cause = if (cause == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(cause) as kotlin.Throwable
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.Throwable(__cause)) }
    return run { _result; true }
}

@ExportedBridge("kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun kotlin_Throwable_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.Throwable()) }
    return run { _result; true }
}

@ExportedBridge("kotlin_Throwable_message_get")
public fun kotlin_Throwable_message_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Throwable
    val _result = run { __self.message }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else _result.objcPtr()
}

@ExportedBridge("kotlin_Throwable_printStackTrace")
public fun kotlin_Throwable_printStackTrace(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Throwable
    val _result = run { __self.printStackTrace() }
    return run { _result; true }
}

@ExportedBridge("kotlin_Throwable_toString")
public fun kotlin_Throwable_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Throwable
    val _result = run { __self.toString() }
    return _result.objcPtr()
}

@ExportedBridge("kotlin_collections_Iterator_hasNext")
public fun kotlin_collections_Iterator_hasNext(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Iterator<kotlin.Any?>
    val _result = run { __self.hasNext() }
    return _result
}

@ExportedBridge("kotlin_collections_Iterator_next")
public fun kotlin_collections_Iterator_next(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Iterator<kotlin.Any?>
    val _result = run { __self.next() }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_coroutines_CoroutineContext_Element_fold__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_coroutines_CoroutineContext_Element_fold__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, initial: kotlin.native.internal.NativePtr, operation: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.coroutines.CoroutineContext.Element
    val __initial = if (initial == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(initial) as kotlin.Any
    val __operation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr)->kotlin.native.internal.NativePtr>(operation);
        { arg0: kotlin.Any?, arg1: kotlin.coroutines.CoroutineContext.Element ->
            val _result = kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0), kotlin.native.internal.ref.createRetainedExternalRCRef(arg1))
            if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.Any
        }
    }
    val _result = run { __self.fold(__initial, __operation) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_coroutines_CoroutineContext_Element_key_get")
public fun kotlin_coroutines_CoroutineContext_Element_key_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.coroutines.CoroutineContext.Element
    val _result = run { __self.key }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_coroutines_CoroutineContext_Element_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__")
public fun kotlin_coroutines_CoroutineContext_Element_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__(self: kotlin.native.internal.NativePtr, key: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.coroutines.CoroutineContext.Element
    val __key = kotlin.native.internal.ref.dereferenceExternalRCRef(key) as kotlin.coroutines.CoroutineContext.Key<kotlin.coroutines.CoroutineContext.Element>
    val _result = run { __self.minusKey(__key) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_coroutines_CoroutineContext_fold__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_coroutines_CoroutineContext_fold__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U28Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__U20anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_ElementU29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, initial: kotlin.native.internal.NativePtr, operation: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.coroutines.CoroutineContext
    val __initial = if (initial == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(initial) as kotlin.Any
    val __operation = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(kotlin.native.internal.NativePtr, kotlin.native.internal.NativePtr)->kotlin.native.internal.NativePtr>(operation);
        { arg0: kotlin.Any?, arg1: kotlin.coroutines.CoroutineContext.Element ->
            val _result = kotlinFun(if (arg0 == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(arg0), kotlin.native.internal.ref.createRetainedExternalRCRef(arg1))
            if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.Any
        }
    }
    val _result = run { __self.fold(__initial, __operation) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_coroutines_CoroutineContext_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__")
public fun kotlin_coroutines_CoroutineContext_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__(self: kotlin.native.internal.NativePtr, key: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.coroutines.CoroutineContext
    val __key = kotlin.native.internal.ref.dereferenceExternalRCRef(key) as kotlin.coroutines.CoroutineContext.Key<kotlin.coroutines.CoroutineContext.Element>
    val _result = run { __self.minusKey(__key) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_coroutines_CoroutineContext_plus__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__")
public fun kotlin_coroutines_CoroutineContext_plus__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_CoroutineContext__(self: kotlin.native.internal.NativePtr, context: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.coroutines.CoroutineContext
    val __context = kotlin.native.internal.ref.dereferenceExternalRCRef(context) as kotlin.coroutines.CoroutineContext
    val _result = run { __self.plus(__context) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_coroutines_cancellation_CancellationException_init_allocate")
public fun kotlin_coroutines_cancellation_CancellationException_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<kotlin.coroutines.cancellation.CancellationException>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_coroutines_cancellation_CancellationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun kotlin_coroutines_cancellation_CancellationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.coroutines.cancellation.CancellationException()) }
    return run { _result; true }
}

@ExportedBridge("kotlin_coroutines_cancellation_CancellationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___")
public fun kotlin_coroutines_cancellation_CancellationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt: kotlin.native.internal.NativePtr, message: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __message = if (message == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.String>(message)
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.coroutines.cancellation.CancellationException(__message)) }
    return run { _result; true }
}

@ExportedBridge("kotlin_coroutines_cancellation_CancellationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___")
public fun kotlin_coroutines_cancellation_CancellationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String__Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt: kotlin.native.internal.NativePtr, message: kotlin.native.internal.NativePtr, cause: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __message = if (message == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.String>(message)
    val __cause = if (cause == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(cause) as kotlin.Throwable
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.coroutines.cancellation.CancellationException(__message, __cause)) }
    return run { _result; true }
}

@ExportedBridge("kotlin_coroutines_cancellation_CancellationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___")
public fun kotlin_coroutines_cancellation_CancellationException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_ExportedKotlinPackages_kotlin_Throwable___(__kt: kotlin.native.internal.NativePtr, cause: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __cause = if (cause == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(cause) as kotlin.Throwable
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.coroutines.cancellation.CancellationException(__cause)) }
    return run { _result; true }
}
