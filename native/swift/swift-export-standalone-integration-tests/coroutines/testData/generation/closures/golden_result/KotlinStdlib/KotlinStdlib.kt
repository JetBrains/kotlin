@file:OptIn(kotlin.ExperimentalStdlibApi::class)
@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Array::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE5ArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Exception::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE9ExceptionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.IllegalStateException::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE21IllegalStateExceptionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.IntArray::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE8IntArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.LongArray::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE9LongArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.NoSuchElementException::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE22NoSuchElementExceptionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.RuntimeException::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE16RuntimeExceptionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Throwable::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE9ThrowableC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.IndexedValue::class, "22ExportedKotlinPackages6kotlinO11collectionsO12KotlinStdlibE12IndexedValueC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.IntIterator::class, "22ExportedKotlinPackages6kotlinO11collectionsO12KotlinStdlibE11IntIteratorC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.LongIterator::class, "22ExportedKotlinPackages6kotlinO11collectionsO12KotlinStdlibE12LongIteratorC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Collection::class, "_Collection")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Iterable::class, "_Iterable")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Iterator::class, "_Iterator")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.MutableCollection::class, "_MutableCollection")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.MutableIterable::class, "_MutableIterable")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.MutableIterator::class, "_MutableIterator")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.coroutines.AbstractCoroutineContextElement::class, "22ExportedKotlinPackages6kotlinO10coroutinesO12KotlinStdlibE31AbstractCoroutineContextElementC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.coroutines.AbstractCoroutineContextKey::class, "22ExportedKotlinPackages6kotlinO10coroutinesO12KotlinStdlibE27AbstractCoroutineContextKeyC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.coroutines.Continuation::class, "_Continuation")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.coroutines.ContinuationInterceptor::class, "_ContinuationInterceptor")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.coroutines.CoroutineContext::class, "_CoroutineContext")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.coroutines.cancellation.CancellationException::class, "22ExportedKotlinPackages6kotlinO10coroutinesO12cancellationO12KotlinStdlibE21CancellationExceptionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.sequences.Sequence::class, "_Sequence")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.time.Duration::class, "22ExportedKotlinPackages6kotlinO4timeO12KotlinStdlibE8DurationC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.time.Duration.Companion::class, "22ExportedKotlinPackages6kotlinO4timeO12KotlinStdlibE8DurationC9CompanionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.coroutines.ContinuationInterceptor.Key::class, "12KotlinStdlib69_ExportedKotlinPackages_kotlin_coroutines_ContinuationInterceptor_KeyC")
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

@ExportedBridge("kotlin_IntArray_get__TypesOfArguments__Swift_Int32__")
public fun kotlin_IntArray_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.IntArray
    val __index = index
    val _result = run { __self.`get`(__index) }
    return _result
}

@ExportedBridge("kotlin_IntArray_iterator")
public fun kotlin_IntArray_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.IntArray
    val _result = run { __self.iterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_IntArray_set__TypesOfArguments__Swift_Int32_Swift_Int32__")
public fun kotlin_IntArray_set__TypesOfArguments__Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int, value: Int): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.IntArray
    val __index = index
    val __value = value
    val _result = run { __self.`set`(__index, __value) }
    return run { _result; true }
}

@ExportedBridge("kotlin_IntArray_size_get")
public fun kotlin_IntArray_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.IntArray
    val _result = run { __self.size }
    return _result
}

@ExportedBridge("kotlin_LongArray_get__TypesOfArguments__Swift_Int32__")
public fun kotlin_LongArray_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.LongArray
    val __index = index
    val _result = run { __self.`get`(__index) }
    return _result
}

@ExportedBridge("kotlin_LongArray_iterator")
public fun kotlin_LongArray_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.LongArray
    val _result = run { __self.iterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_LongArray_set__TypesOfArguments__Swift_Int32_Swift_Int64__")
public fun kotlin_LongArray_set__TypesOfArguments__Swift_Int32_Swift_Int64__(self: kotlin.native.internal.NativePtr, index: Int, value: Long): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.LongArray
    val __index = index
    val __value = value
    val _result = run { __self.`set`(__index, __value) }
    return run { _result; true }
}

@ExportedBridge("kotlin_LongArray_size_get")
public fun kotlin_LongArray_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.LongArray
    val _result = run { __self.size }
    return _result
}

@ExportedBridge("kotlin_NoSuchElementException_init_allocate")
public fun kotlin_NoSuchElementException_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<kotlin.NoSuchElementException>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_NoSuchElementException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun kotlin_NoSuchElementException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.NoSuchElementException()) }
    return run { _result; true }
}

@ExportedBridge("kotlin_NoSuchElementException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___")
public fun kotlin_NoSuchElementException_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Optional_Swift_String___(__kt: kotlin.native.internal.NativePtr, message: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __message = if (message == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.String>(message)
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.NoSuchElementException(__message)) }
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
@OptIn(kotlin.experimental.ExperimentalNativeApi::class)
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

@ExportedBridge("kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Collection<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.contains(__element) }
    return _result
}

@ExportedBridge("kotlin_collections_Collection_isEmpty")
public fun kotlin_collections_Collection_isEmpty(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.isEmpty() }
    return _result
}

@ExportedBridge("kotlin_collections_Collection_iterator")
public fun kotlin_collections_Collection_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.iterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_Collection_size_get")
public fun kotlin_collections_Collection_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.size }
    return _result
}

@ExportedBridge("kotlin_collections_IndexedValue_copy__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_IndexedValue_copy__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, index: Int, value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.IndexedValue<kotlin.Any?>
    val __index = index
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Any
    val _result = run { __self.copy(__index, __value) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_IndexedValue_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_IndexedValue_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.IndexedValue<kotlin.Any?>
    val __other = if (other == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Any
    val _result = run { __self.equals(__other) }
    return _result
}

@ExportedBridge("kotlin_collections_IndexedValue_hashCode")
public fun kotlin_collections_IndexedValue_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.IndexedValue<kotlin.Any?>
    val _result = run { __self.hashCode() }
    return _result
}

@ExportedBridge("kotlin_collections_IndexedValue_index_get")
public fun kotlin_collections_IndexedValue_index_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.IndexedValue<kotlin.Any?>
    val _result = run { __self.index }
    return _result
}

@ExportedBridge("kotlin_collections_IndexedValue_init_allocate")
public fun kotlin_collections_IndexedValue_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<kotlin.collections.IndexedValue<kotlin.Any?>>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_IndexedValue_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_IndexedValue_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(__kt: kotlin.native.internal.NativePtr, index: Int, value: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __index = index
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Any
    val _result = run { kotlin.native.internal.initInstance(____kt, kotlin.collections.IndexedValue<kotlin.Any?>(__index, __value)) }
    return run { _result; true }
}

@ExportedBridge("kotlin_collections_IndexedValue_toString")
public fun kotlin_collections_IndexedValue_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.IndexedValue<kotlin.Any?>
    val _result = run { __self.toString() }
    return _result.objcPtr()
}

@ExportedBridge("kotlin_collections_IndexedValue_value_get")
public fun kotlin_collections_IndexedValue_value_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.IndexedValue<kotlin.Any?>
    val _result = run { __self.value }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_IntIterator_next")
public fun kotlin_collections_IntIterator_next(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.IntIterator
    val _result = run { __self.next() }
    return _result
}

@ExportedBridge("kotlin_collections_IntIterator_nextInt")
public fun kotlin_collections_IntIterator_nextInt(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.IntIterator
    val _result = run { __self.nextInt() }
    return _result
}

@ExportedBridge("kotlin_collections_Iterable_iterator")
public fun kotlin_collections_Iterable_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Iterable<kotlin.Any?>
    val _result = run { __self.iterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
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

@ExportedBridge("kotlin_collections_LongIterator_next")
public fun kotlin_collections_LongIterator_next(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.LongIterator
    val _result = run { __self.next() }
    return _result
}

@ExportedBridge("kotlin_collections_LongIterator_nextLong")
public fun kotlin_collections_LongIterator_nextLong(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.LongIterator
    val _result = run { __self.nextLong() }
    return _result
}

@ExportedBridge("kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableCollection<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.add(__element) }
    return _result
}

@ExportedBridge("kotlin_collections_MutableCollection_clear")
public fun kotlin_collections_MutableCollection_clear(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableCollection<kotlin.Any?>
    val _result = run { __self.clear() }
    return run { _result; true }
}

@ExportedBridge("kotlin_collections_MutableCollection_iterator")
public fun kotlin_collections_MutableCollection_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableCollection<kotlin.Any?>
    val _result = run { __self.iterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_MutableCollection_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_MutableCollection_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableCollection<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.remove(__element) }
    return _result
}

@ExportedBridge("kotlin_collections_MutableIterable_iterator")
public fun kotlin_collections_MutableIterable_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableIterable<kotlin.Any?>
    val _result = run { __self.iterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_MutableIterator_remove")
public fun kotlin_collections_MutableIterator_remove(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableIterator<kotlin.Any?>
    val _result = run { __self.remove() }
    return run { _result; true }
}

@ExportedBridge("kotlin_coroutines_AbstractCoroutineContextElement_key_get")
public fun kotlin_coroutines_AbstractCoroutineContextElement_key_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.coroutines.AbstractCoroutineContextElement
    val _result = run { __self.key }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_coroutines_ContinuationInterceptor_Key_get")
public fun kotlin_coroutines_ContinuationInterceptor_Key_get(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.coroutines.ContinuationInterceptor.Key }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_coroutines_ContinuationInterceptor_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__")
public fun kotlin_coroutines_ContinuationInterceptor_minusKey__TypesOfArguments__anyU20KotlinStdlib__ExportedKotlinPackages_kotlin_coroutines_CoroutineContext_Key__(self: kotlin.native.internal.NativePtr, key: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.coroutines.ContinuationInterceptor
    val __key = kotlin.native.internal.ref.dereferenceExternalRCRef(key) as kotlin.coroutines.CoroutineContext.Key<kotlin.coroutines.CoroutineContext.Element>
    val _result = run { __self.minusKey(__key) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_coroutines_ContinuationInterceptor_releaseInterceptedContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation__")
public fun kotlin_coroutines_ContinuationInterceptor_releaseInterceptedContinuation__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_coroutines_Continuation__(self: kotlin.native.internal.NativePtr, continuation: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.coroutines.ContinuationInterceptor
    val __continuation = kotlin.native.internal.ref.dereferenceExternalRCRef(continuation) as kotlin.coroutines.Continuation<kotlin.Any?>
    val _result = run { __self.releaseInterceptedContinuation(__continuation) }
    return run { _result; true }
}

@ExportedBridge("kotlin_coroutines_Continuation_context_get")
public fun kotlin_coroutines_Continuation_context_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.coroutines.Continuation<kotlin.Any?>
    val _result = run { __self.context }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
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
    val _result = run { __self.fold<kotlin.Any?>(__initial, __operation) }
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
    val _result = run { __self.fold<kotlin.Any?>(__initial, __operation) }
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

@ExportedBridge("kotlin_sequences_Sequence_iterator")
public fun kotlin_sequences_Sequence_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.sequences.Sequence<kotlin.Any?>
    val _result = run { __self.iterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_DurationUnit_DAYS")
public fun kotlin_time_DurationUnit_DAYS(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.time.DurationUnit.DAYS }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_DurationUnit_HOURS")
public fun kotlin_time_DurationUnit_HOURS(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.time.DurationUnit.HOURS }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_DurationUnit_MICROSECONDS")
public fun kotlin_time_DurationUnit_MICROSECONDS(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.time.DurationUnit.MICROSECONDS }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_DurationUnit_MILLISECONDS")
public fun kotlin_time_DurationUnit_MILLISECONDS(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.time.DurationUnit.MILLISECONDS }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_DurationUnit_MINUTES")
public fun kotlin_time_DurationUnit_MINUTES(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.time.DurationUnit.MINUTES }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_DurationUnit_NANOSECONDS")
public fun kotlin_time_DurationUnit_NANOSECONDS(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.time.DurationUnit.NANOSECONDS }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_DurationUnit_SECONDS")
public fun kotlin_time_DurationUnit_SECONDS(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.time.DurationUnit.SECONDS }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_DurationUnit_ordinal")
public fun kotlin_time_DurationUnit_ordinal(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.DurationUnit
    val _result = run { __self.ordinal }
    return _result
}

@ExportedBridge("kotlin_time_Duration_Companion_INFINITE_get")
public fun kotlin_time_Duration_Companion_INFINITE_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val _result = run { __self.INFINITE }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_ZERO_get")
public fun kotlin_time_Duration_Companion_ZERO_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val _result = run { __self.ZERO }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_convert__TypesOfArguments__Swift_Double_ExportedKotlinPackages_kotlin_time_DurationUnit_ExportedKotlinPackages_kotlin_time_DurationUnit__")
@OptIn(kotlin.time.ExperimentalTime::class)
public fun kotlin_time_Duration_Companion_convert__TypesOfArguments__Swift_Double_ExportedKotlinPackages_kotlin_time_DurationUnit_ExportedKotlinPackages_kotlin_time_DurationUnit__(self: kotlin.native.internal.NativePtr, value: Double, sourceUnit: kotlin.native.internal.NativePtr, targetUnit: kotlin.native.internal.NativePtr): Double {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __value = value
    val __sourceUnit = kotlin.native.internal.ref.dereferenceExternalRCRef(sourceUnit) as kotlin.time.DurationUnit
    val __targetUnit = kotlin.native.internal.ref.dereferenceExternalRCRef(targetUnit) as kotlin.time.DurationUnit
    val _result = run { __self.convert(__value, __sourceUnit, __targetUnit) }
    return _result
}

@ExportedBridge("kotlin_time_Duration_Companion_days_get__TypesOfArgumentsE__Swift_Int32__")
public fun kotlin_time_Duration_Companion_days_get__TypesOfArgumentsE__Swift_Int32__(self: kotlin.native.internal.NativePtr, `receiver`: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.days } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_days_get__TypesOfArgumentsE__Swift_Int64__")
public fun kotlin_time_Duration_Companion_days_get__TypesOfArgumentsE__Swift_Int64__(self: kotlin.native.internal.NativePtr, `receiver`: Long): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.days } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_days_get__TypesOfArgumentsE__Swift_Double__")
public fun kotlin_time_Duration_Companion_days_get__TypesOfArgumentsE__Swift_Double__(self: kotlin.native.internal.NativePtr, `receiver`: Double): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.days } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_get")
public fun kotlin_time_Duration_Companion_get(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.time.Duration.Companion }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_hours_get__TypesOfArgumentsE__Swift_Int32__")
public fun kotlin_time_Duration_Companion_hours_get__TypesOfArgumentsE__Swift_Int32__(self: kotlin.native.internal.NativePtr, `receiver`: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.hours } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_hours_get__TypesOfArgumentsE__Swift_Int64__")
public fun kotlin_time_Duration_Companion_hours_get__TypesOfArgumentsE__Swift_Int64__(self: kotlin.native.internal.NativePtr, `receiver`: Long): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.hours } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_hours_get__TypesOfArgumentsE__Swift_Double__")
public fun kotlin_time_Duration_Companion_hours_get__TypesOfArgumentsE__Swift_Double__(self: kotlin.native.internal.NativePtr, `receiver`: Double): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.hours } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_microseconds_get__TypesOfArgumentsE__Swift_Int32__")
public fun kotlin_time_Duration_Companion_microseconds_get__TypesOfArgumentsE__Swift_Int32__(self: kotlin.native.internal.NativePtr, `receiver`: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.microseconds } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_microseconds_get__TypesOfArgumentsE__Swift_Int64__")
public fun kotlin_time_Duration_Companion_microseconds_get__TypesOfArgumentsE__Swift_Int64__(self: kotlin.native.internal.NativePtr, `receiver`: Long): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.microseconds } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_microseconds_get__TypesOfArgumentsE__Swift_Double__")
public fun kotlin_time_Duration_Companion_microseconds_get__TypesOfArgumentsE__Swift_Double__(self: kotlin.native.internal.NativePtr, `receiver`: Double): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.microseconds } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_milliseconds_get__TypesOfArgumentsE__Swift_Int32__")
public fun kotlin_time_Duration_Companion_milliseconds_get__TypesOfArgumentsE__Swift_Int32__(self: kotlin.native.internal.NativePtr, `receiver`: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.milliseconds } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_milliseconds_get__TypesOfArgumentsE__Swift_Int64__")
public fun kotlin_time_Duration_Companion_milliseconds_get__TypesOfArgumentsE__Swift_Int64__(self: kotlin.native.internal.NativePtr, `receiver`: Long): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.milliseconds } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_milliseconds_get__TypesOfArgumentsE__Swift_Double__")
public fun kotlin_time_Duration_Companion_milliseconds_get__TypesOfArgumentsE__Swift_Double__(self: kotlin.native.internal.NativePtr, `receiver`: Double): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.milliseconds } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_minutes_get__TypesOfArgumentsE__Swift_Int32__")
public fun kotlin_time_Duration_Companion_minutes_get__TypesOfArgumentsE__Swift_Int32__(self: kotlin.native.internal.NativePtr, `receiver`: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.minutes } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_minutes_get__TypesOfArgumentsE__Swift_Int64__")
public fun kotlin_time_Duration_Companion_minutes_get__TypesOfArgumentsE__Swift_Int64__(self: kotlin.native.internal.NativePtr, `receiver`: Long): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.minutes } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_minutes_get__TypesOfArgumentsE__Swift_Double__")
public fun kotlin_time_Duration_Companion_minutes_get__TypesOfArgumentsE__Swift_Double__(self: kotlin.native.internal.NativePtr, `receiver`: Double): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.minutes } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_nanoseconds_get__TypesOfArgumentsE__Swift_Int32__")
public fun kotlin_time_Duration_Companion_nanoseconds_get__TypesOfArgumentsE__Swift_Int32__(self: kotlin.native.internal.NativePtr, `receiver`: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.nanoseconds } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_nanoseconds_get__TypesOfArgumentsE__Swift_Int64__")
public fun kotlin_time_Duration_Companion_nanoseconds_get__TypesOfArgumentsE__Swift_Int64__(self: kotlin.native.internal.NativePtr, `receiver`: Long): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.nanoseconds } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_nanoseconds_get__TypesOfArgumentsE__Swift_Double__")
public fun kotlin_time_Duration_Companion_nanoseconds_get__TypesOfArgumentsE__Swift_Double__(self: kotlin.native.internal.NativePtr, `receiver`: Double): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.nanoseconds } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_parse__TypesOfArguments__Swift_String__")
public fun kotlin_time_Duration_Companion_parse__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __value = interpretObjCPointer<kotlin.String>(value)
    val _result = run { __self.parse(__value) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_parseIsoString__TypesOfArguments__Swift_String__")
public fun kotlin_time_Duration_Companion_parseIsoString__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __value = interpretObjCPointer<kotlin.String>(value)
    val _result = run { __self.parseIsoString(__value) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_parseIsoStringOrNull__TypesOfArguments__Swift_String__")
public fun kotlin_time_Duration_Companion_parseIsoStringOrNull__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __value = interpretObjCPointer<kotlin.String>(value)
    val _result = run { __self.parseIsoStringOrNull(__value) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_parseOrNull__TypesOfArguments__Swift_String__")
public fun kotlin_time_Duration_Companion_parseOrNull__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __value = interpretObjCPointer<kotlin.String>(value)
    val _result = run { __self.parseOrNull(__value) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_seconds_get__TypesOfArgumentsE__Swift_Int32__")
public fun kotlin_time_Duration_Companion_seconds_get__TypesOfArgumentsE__Swift_Int32__(self: kotlin.native.internal.NativePtr, `receiver`: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.seconds } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_seconds_get__TypesOfArgumentsE__Swift_Int64__")
public fun kotlin_time_Duration_Companion_seconds_get__TypesOfArgumentsE__Swift_Int64__(self: kotlin.native.internal.NativePtr, `receiver`: Long): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.seconds } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_Companion_seconds_get__TypesOfArgumentsE__Swift_Double__")
public fun kotlin_time_Duration_Companion_seconds_get__TypesOfArgumentsE__Swift_Double__(self: kotlin.native.internal.NativePtr, `receiver`: Double): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration.Companion
    val __receiver = `receiver`
    val _result = run { __self.run { __receiver.seconds } }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_absoluteValue_get")
public fun kotlin_time_Duration_absoluteValue_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val _result = run { __self.absoluteValue }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_compareTo__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__")
public fun kotlin_time_Duration_compareTo__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val __other = kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.time.Duration
    val _result = run { __self.compareTo(__other) }
    return _result
}

@ExportedBridge("kotlin_time_Duration_div__TypesOfArguments__Swift_Int32__")
public fun kotlin_time_Duration_div__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, scale: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val __scale = scale
    val _result = run { __self.div(__scale) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_div__TypesOfArguments__Swift_Double__")
public fun kotlin_time_Duration_div__TypesOfArguments__Swift_Double__(self: kotlin.native.internal.NativePtr, scale: Double): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val __scale = scale
    val _result = run { __self.div(__scale) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_div__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__")
public fun kotlin_time_Duration_div__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Double {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val __other = kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.time.Duration
    val _result = run { __self.div(__other) }
    return _result
}

@ExportedBridge("kotlin_time_Duration_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_time_Duration_equals__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val __other = if (other == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Any
    val _result = run { __self.equals(__other) }
    return _result
}

@ExportedBridge("kotlin_time_Duration_hashCode")
public fun kotlin_time_Duration_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val _result = run { __self.hashCode() }
    return _result
}

@ExportedBridge("kotlin_time_Duration_inWholeDays_get")
public fun kotlin_time_Duration_inWholeDays_get(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val _result = run { __self.inWholeDays }
    return _result
}

@ExportedBridge("kotlin_time_Duration_inWholeHours_get")
public fun kotlin_time_Duration_inWholeHours_get(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val _result = run { __self.inWholeHours }
    return _result
}

@ExportedBridge("kotlin_time_Duration_inWholeMicroseconds_get")
public fun kotlin_time_Duration_inWholeMicroseconds_get(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val _result = run { __self.inWholeMicroseconds }
    return _result
}

@ExportedBridge("kotlin_time_Duration_inWholeMilliseconds_get")
public fun kotlin_time_Duration_inWholeMilliseconds_get(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val _result = run { __self.inWholeMilliseconds }
    return _result
}

@ExportedBridge("kotlin_time_Duration_inWholeMinutes_get")
public fun kotlin_time_Duration_inWholeMinutes_get(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val _result = run { __self.inWholeMinutes }
    return _result
}

@ExportedBridge("kotlin_time_Duration_inWholeNanoseconds_get")
public fun kotlin_time_Duration_inWholeNanoseconds_get(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val _result = run { __self.inWholeNanoseconds }
    return _result
}

@ExportedBridge("kotlin_time_Duration_inWholeSeconds_get")
public fun kotlin_time_Duration_inWholeSeconds_get(self: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val _result = run { __self.inWholeSeconds }
    return _result
}

@ExportedBridge("kotlin_time_Duration_isFinite")
public fun kotlin_time_Duration_isFinite(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val _result = run { __self.isFinite() }
    return _result
}

@ExportedBridge("kotlin_time_Duration_isInfinite")
public fun kotlin_time_Duration_isInfinite(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val _result = run { __self.isInfinite() }
    return _result
}

@ExportedBridge("kotlin_time_Duration_isNegative")
public fun kotlin_time_Duration_isNegative(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val _result = run { __self.isNegative() }
    return _result
}

@ExportedBridge("kotlin_time_Duration_isPositive")
public fun kotlin_time_Duration_isPositive(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val _result = run { __self.isPositive() }
    return _result
}

@ExportedBridge("kotlin_time_Duration_minus__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__")
public fun kotlin_time_Duration_minus__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val __other = kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.time.Duration
    val _result = run { __self.minus(__other) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_plus__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__")
public fun kotlin_time_Duration_plus__TypesOfArguments__ExportedKotlinPackages_kotlin_time_Duration__(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val __other = kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.time.Duration
    val _result = run { __self.plus(__other) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_times__TypesOfArguments__Swift_Int32__")
public fun kotlin_time_Duration_times__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, scale: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val __scale = scale
    val _result = run { __self.times(__scale) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_times__TypesOfArguments__Swift_Double__")
public fun kotlin_time_Duration_times__TypesOfArguments__Swift_Double__(self: kotlin.native.internal.NativePtr, scale: Double): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val __scale = scale
    val _result = run { __self.times(__scale) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, action: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val __action = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Long, Int, Int, Int, Int)->kotlin.native.internal.NativePtr>(action);
        { arg0: Long, arg1: Int, arg2: Int, arg3: Int, arg4: Int ->
            val _result = kotlinFun(arg0, arg1, arg2, arg3, arg4)
            if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.Any
        }
    }
    val _result = run { __self.toComponents<kotlin.Any?>(__action) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32_U20Swift_Int32U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, action: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val __action = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Long, Int, Int, Int)->kotlin.native.internal.NativePtr>(action);
        { arg0: Long, arg1: Int, arg2: Int, arg3: Int ->
            val _result = kotlinFun(arg0, arg1, arg2, arg3)
            if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.Any
        }
    }
    val _result = run { __self.toComponents<kotlin.Any?>(__action) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32_U20Swift_Int32U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, action: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val __action = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Long, Int, Int)->kotlin.native.internal.NativePtr>(action);
        { arg0: Long, arg1: Int, arg2: Int ->
            val _result = kotlinFun(arg0, arg1, arg2)
            if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.Any
        }
    }
    val _result = run { __self.toComponents<kotlin.Any?>(__action) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_time_Duration_toComponents__TypesOfArguments__U28Swift_Int64_U20Swift_Int32U29202D_U20Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, action: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val __action = run {
        val kotlinFun = convertBlockPtrToKotlinFunction<(Long, Int)->kotlin.native.internal.NativePtr>(action);
        { arg0: Long, arg1: Int ->
            val _result = kotlinFun(arg0, arg1)
            if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.Any
        }
    }
    val _result = run { __self.toComponents<kotlin.Any?>(__action) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_time_Duration_toDouble__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit__")
public fun kotlin_time_Duration_toDouble__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit__(self: kotlin.native.internal.NativePtr, unit: kotlin.native.internal.NativePtr): Double {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val __unit = kotlin.native.internal.ref.dereferenceExternalRCRef(unit) as kotlin.time.DurationUnit
    val _result = run { __self.toDouble(__unit) }
    return _result
}

@ExportedBridge("kotlin_time_Duration_toInt__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit__")
public fun kotlin_time_Duration_toInt__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit__(self: kotlin.native.internal.NativePtr, unit: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val __unit = kotlin.native.internal.ref.dereferenceExternalRCRef(unit) as kotlin.time.DurationUnit
    val _result = run { __self.toInt(__unit) }
    return _result
}

@ExportedBridge("kotlin_time_Duration_toIsoString")
public fun kotlin_time_Duration_toIsoString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val _result = run { __self.toIsoString() }
    return _result.objcPtr()
}

@ExportedBridge("kotlin_time_Duration_toLong__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit__")
public fun kotlin_time_Duration_toLong__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit__(self: kotlin.native.internal.NativePtr, unit: kotlin.native.internal.NativePtr): Long {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val __unit = kotlin.native.internal.ref.dereferenceExternalRCRef(unit) as kotlin.time.DurationUnit
    val _result = run { __self.toLong(__unit) }
    return _result
}

@ExportedBridge("kotlin_time_Duration_toString")
public fun kotlin_time_Duration_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val _result = run { __self.toString() }
    return _result.objcPtr()
}

@ExportedBridge("kotlin_time_Duration_toString__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit_Swift_Int32__")
public fun kotlin_time_Duration_toString__TypesOfArguments__ExportedKotlinPackages_kotlin_time_DurationUnit_Swift_Int32__(self: kotlin.native.internal.NativePtr, unit: kotlin.native.internal.NativePtr, decimals: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val __unit = kotlin.native.internal.ref.dereferenceExternalRCRef(unit) as kotlin.time.DurationUnit
    val __decimals = decimals
    val _result = run { __self.toString(__unit, __decimals) }
    return _result.objcPtr()
}

@ExportedBridge("kotlin_time_Duration_unaryMinus")
public fun kotlin_time_Duration_unaryMinus(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.time.Duration
    val _result = run { __self.unaryMinus() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
