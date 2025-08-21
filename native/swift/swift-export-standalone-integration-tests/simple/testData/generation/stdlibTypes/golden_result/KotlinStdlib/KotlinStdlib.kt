@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.ByteArray::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE9ByteArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.CharArray::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE9CharArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.CharSequence::class, "_CharSequence")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.ByteIterator::class, "22ExportedKotlinPackages6kotlinO11collectionsO12KotlinStdlibE12ByteIteratorC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.CharIterator::class, "22ExportedKotlinPackages6kotlinO11collectionsO12KotlinStdlibE12CharIteratorC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.text.StringBuilder::class, "22ExportedKotlinPackages6kotlinO4textO12KotlinStdlibE13StringBuilderC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.text.Appendable::class, "_Appendable")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("kotlin_ByteArray_get__TypesOfArguments__Swift_Int32__")
public fun kotlin_ByteArray_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): Byte {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ByteArray
    val __index = index
    val _result = __self.`get`(__index)
    return _result
}

@ExportedBridge("kotlin_ByteArray_iterator")
public fun kotlin_ByteArray_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ByteArray
    val _result = __self.iterator()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_ByteArray_set__TypesOfArguments__Swift_Int32_Swift_Int8__")
public fun kotlin_ByteArray_set__TypesOfArguments__Swift_Int32_Swift_Int8__(self: kotlin.native.internal.NativePtr, index: Int, value: Byte): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ByteArray
    val __index = index
    val __value = value
    __self.`set`(__index, __value)
}

@ExportedBridge("kotlin_ByteArray_size_get")
public fun kotlin_ByteArray_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.ByteArray
    val _result = __self.size
    return _result
}

@ExportedBridge("kotlin_CharArray_get__TypesOfArguments__Swift_Int32__")
public fun kotlin_CharArray_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): Char {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.CharArray
    val __index = index
    val _result = __self.`get`(__index)
    return _result
}

@ExportedBridge("kotlin_CharArray_iterator")
public fun kotlin_CharArray_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.CharArray
    val _result = __self.iterator()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_CharArray_set__TypesOfArguments__Swift_Int32_Swift_Unicode_UTF16_CodeUnit__")
public fun kotlin_CharArray_set__TypesOfArguments__Swift_Int32_Swift_Unicode_UTF16_CodeUnit__(self: kotlin.native.internal.NativePtr, index: Int, value: Char): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.CharArray
    val __index = index
    val __value = value
    __self.`set`(__index, __value)
}

@ExportedBridge("kotlin_CharArray_size_get")
public fun kotlin_CharArray_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.CharArray
    val _result = __self.size
    return _result
}

@ExportedBridge("kotlin_CharSequence_get__TypesOfArguments__Swift_Int32__")
public fun kotlin_CharSequence_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): Char {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.CharSequence
    val __index = index
    val _result = __self.`get`(__index)
    return _result
}

@ExportedBridge("kotlin_CharSequence_length_get")
public fun kotlin_CharSequence_length_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.CharSequence
    val _result = __self.length
    return _result
}

@ExportedBridge("kotlin_CharSequence_subSequence__TypesOfArguments__Swift_Int32_Swift_Int32__")
public fun kotlin_CharSequence_subSequence__TypesOfArguments__Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, startIndex: Int, endIndex: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.CharSequence
    val __startIndex = startIndex
    val __endIndex = endIndex
    val _result = __self.subSequence(__startIndex, __endIndex)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_ByteIterator_next")
public fun kotlin_collections_ByteIterator_next(self: kotlin.native.internal.NativePtr): Byte {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ByteIterator
    val _result = __self.next()
    return _result
}

@ExportedBridge("kotlin_collections_ByteIterator_nextByte")
public fun kotlin_collections_ByteIterator_nextByte(self: kotlin.native.internal.NativePtr): Byte {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ByteIterator
    val _result = __self.nextByte()
    return _result
}

@ExportedBridge("kotlin_collections_CharIterator_next")
public fun kotlin_collections_CharIterator_next(self: kotlin.native.internal.NativePtr): Char {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.CharIterator
    val _result = __self.next()
    return _result
}

@ExportedBridge("kotlin_collections_CharIterator_nextChar")
public fun kotlin_collections_CharIterator_nextChar(self: kotlin.native.internal.NativePtr): Char {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.CharIterator
    val _result = __self.nextChar()
    return _result
}

@ExportedBridge("kotlin_text_Appendable_append__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__")
public fun kotlin_text_Appendable_append__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self: kotlin.native.internal.NativePtr, value: Char): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.Appendable
    val __value = value
    val _result = __self.append(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_Appendable_append__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlin_CharSequence___")
public fun kotlin_text_Appendable_append__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlin_CharSequence___(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.Appendable
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.CharSequence
    val _result = __self.append(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_Appendable_append__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlin_CharSequence__Swift_Int32_Swift_Int32__")
public fun kotlin_text_Appendable_append__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlin_CharSequence__Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr, startIndex: Int, endIndex: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.Appendable
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.CharSequence
    val __startIndex = startIndex
    val __endIndex = endIndex
    val _result = __self.append(__value, __startIndex, __endIndex)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__")
public fun kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Unicode_UTF16_CodeUnit__(self: kotlin.native.internal.NativePtr, value: Char): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __value = value
    val _result = __self.append(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlin_CharSequence___")
public fun kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlin_CharSequence___(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.CharSequence
    val _result = __self.append(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlin_CharSequence__Swift_Int32_Swift_Int32__")
public fun kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlin_CharSequence__Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr, startIndex: Int, endIndex: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.CharSequence
    val __startIndex = startIndex
    val __endIndex = endIndex
    val _result = __self.append(__value, __startIndex, __endIndex)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Any
    val _result = __self.append(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Bool__")
public fun kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Bool__(self: kotlin.native.internal.NativePtr, value: Boolean): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __value = value
    val _result = __self.append(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Int8__")
public fun kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Int8__(self: kotlin.native.internal.NativePtr, value: Byte): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __value = value
    val _result = __self.append(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Int16__")
public fun kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Int16__(self: kotlin.native.internal.NativePtr, value: Short): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __value = value
    val _result = __self.append(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Int32__")
public fun kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, value: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __value = value
    val _result = __self.append(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Int64__")
public fun kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Int64__(self: kotlin.native.internal.NativePtr, value: Long): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __value = value
    val _result = __self.append(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Float__")
public fun kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Float__(self: kotlin.native.internal.NativePtr, value: Float): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __value = value
    val _result = __self.append(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Double__")
public fun kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Double__(self: kotlin.native.internal.NativePtr, value: Double): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __value = value
    val _result = __self.append(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_append__TypesOfArguments__ExportedKotlinPackages_kotlin_CharArray__")
public fun kotlin_text_StringBuilder_append__TypesOfArguments__ExportedKotlinPackages_kotlin_CharArray__(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __value = kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.CharArray
    val _result = __self.append(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Optional_Swift_String___")
public fun kotlin_text_StringBuilder_append__TypesOfArguments__Swift_Optional_Swift_String___(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.String>(value)
    val _result = __self.append(__value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_appendRange__TypesOfArguments__ExportedKotlinPackages_kotlin_CharArray_Swift_Int32_Swift_Int32__")
public fun kotlin_text_StringBuilder_appendRange__TypesOfArguments__ExportedKotlinPackages_kotlin_CharArray_Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr, startIndex: Int, endIndex: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __value = kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.CharArray
    val __startIndex = startIndex
    val __endIndex = endIndex
    val _result = __self.appendRange(__value, __startIndex, __endIndex)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_appendRange__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_CharSequence_Swift_Int32_Swift_Int32__")
public fun kotlin_text_StringBuilder_appendRange__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_CharSequence_Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr, startIndex: Int, endIndex: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __value = kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.CharSequence
    val __startIndex = startIndex
    val __endIndex = endIndex
    val _result = __self.appendRange(__value, __startIndex, __endIndex)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_capacity")
public fun kotlin_text_StringBuilder_capacity(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val _result = __self.capacity()
    return _result
}

@ExportedBridge("kotlin_text_StringBuilder_deleteAt__TypesOfArguments__Swift_Int32__")
public fun kotlin_text_StringBuilder_deleteAt__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __index = index
    val _result = __self.deleteAt(__index)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_deleteRange__TypesOfArguments__Swift_Int32_Swift_Int32__")
public fun kotlin_text_StringBuilder_deleteRange__TypesOfArguments__Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, startIndex: Int, endIndex: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __startIndex = startIndex
    val __endIndex = endIndex
    val _result = __self.deleteRange(__startIndex, __endIndex)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_ensureCapacity__TypesOfArguments__Swift_Int32__")
public fun kotlin_text_StringBuilder_ensureCapacity__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, minimumCapacity: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __minimumCapacity = minimumCapacity
    __self.ensureCapacity(__minimumCapacity)
}

@ExportedBridge("kotlin_text_StringBuilder_get__TypesOfArguments__Swift_Int32__")
public fun kotlin_text_StringBuilder_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): Char {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __index = index
    val _result = __self.`get`(__index)
    return _result
}

@ExportedBridge("kotlin_text_StringBuilder_indexOf__TypesOfArguments__Swift_String__")
public fun kotlin_text_StringBuilder_indexOf__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, string: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __string = interpretObjCPointer<kotlin.String>(string)
    val _result = __self.indexOf(__string)
    return _result
}

@ExportedBridge("kotlin_text_StringBuilder_indexOf__TypesOfArguments__Swift_String_Swift_Int32__")
public fun kotlin_text_StringBuilder_indexOf__TypesOfArguments__Swift_String_Swift_Int32__(self: kotlin.native.internal.NativePtr, string: kotlin.native.internal.NativePtr, startIndex: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __string = interpretObjCPointer<kotlin.String>(string)
    val __startIndex = startIndex
    val _result = __self.indexOf(__string, __startIndex)
    return _result
}

@ExportedBridge("kotlin_text_StringBuilder_init_allocate")
public fun kotlin_text_StringBuilder_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = kotlin.native.internal.createUninitializedInstance<kotlin.text.StringBuilder>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
public fun kotlin_text_StringBuilder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    kotlin.native.internal.initInstance(____kt, kotlin.text.StringBuilder())
}

@ExportedBridge("kotlin_text_StringBuilder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__")
public fun kotlin_text_StringBuilder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_Int32__(__kt: kotlin.native.internal.NativePtr, capacity: Int): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __capacity = capacity
    kotlin.native.internal.initInstance(____kt, kotlin.text.StringBuilder(__capacity))
}

@ExportedBridge("kotlin_text_StringBuilder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__")
public fun kotlin_text_StringBuilder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String__(__kt: kotlin.native.internal.NativePtr, content: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __content = interpretObjCPointer<kotlin.String>(content)
    kotlin.native.internal.initInstance(____kt, kotlin.text.StringBuilder(__content))
}

@ExportedBridge("kotlin_text_StringBuilder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_CharSequence__")
public fun kotlin_text_StringBuilder_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_CharSequence__(__kt: kotlin.native.internal.NativePtr, content: kotlin.native.internal.NativePtr): Unit {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __content = kotlin.native.internal.ref.dereferenceExternalRCRef(content) as kotlin.CharSequence
    kotlin.native.internal.initInstance(____kt, kotlin.text.StringBuilder(__content))
}

@ExportedBridge("kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Bool__")
public fun kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Bool__(self: kotlin.native.internal.NativePtr, index: Int, value: Boolean): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __index = index
    val __value = value
    val _result = __self.insert(__index, __value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Int8__")
public fun kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Int8__(self: kotlin.native.internal.NativePtr, index: Int, value: Byte): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __index = index
    val __value = value
    val _result = __self.insert(__index, __value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Int16__")
public fun kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Int16__(self: kotlin.native.internal.NativePtr, index: Int, value: Short): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __index = index
    val __value = value
    val _result = __self.insert(__index, __value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Int32__")
public fun kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int, value: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __index = index
    val __value = value
    val _result = __self.insert(__index, __value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Int64__")
public fun kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Int64__(self: kotlin.native.internal.NativePtr, index: Int, value: Long): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __index = index
    val __value = value
    val _result = __self.insert(__index, __value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Float__")
public fun kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Float__(self: kotlin.native.internal.NativePtr, index: Int, value: Float): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __index = index
    val __value = value
    val _result = __self.insert(__index, __value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Double__")
public fun kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Double__(self: kotlin.native.internal.NativePtr, index: Int, value: Double): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __index = index
    val __value = value
    val _result = __self.insert(__index, __value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Unicode_UTF16_CodeUnit__")
public fun kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Unicode_UTF16_CodeUnit__(self: kotlin.native.internal.NativePtr, index: Int, value: Char): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __index = index
    val __value = value
    val _result = __self.insert(__index, __value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_ExportedKotlinPackages_kotlin_CharArray__")
public fun kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_ExportedKotlinPackages_kotlin_CharArray__(self: kotlin.native.internal.NativePtr, index: Int, value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __index = index
    val __value = kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.CharArray
    val _result = __self.insert(__index, __value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20ExportedKotlinPackages_kotlin_CharSequence___")
public fun kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20ExportedKotlinPackages_kotlin_CharSequence___(self: kotlin.native.internal.NativePtr, index: Int, value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __index = index
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.CharSequence
    val _result = __self.insert(__index, __value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, index: Int, value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __index = index
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Any
    val _result = __self.insert(__index, __value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Optional_Swift_String___")
public fun kotlin_text_StringBuilder_insert__TypesOfArguments__Swift_Int32_Swift_Optional_Swift_String___(self: kotlin.native.internal.NativePtr, index: Int, value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __index = index
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else interpretObjCPointer<kotlin.String>(value)
    val _result = __self.insert(__index, __value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_insertRange__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_CharSequence_Swift_Int32_Swift_Int32__")
public fun kotlin_text_StringBuilder_insertRange__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_CharSequence_Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int, value: kotlin.native.internal.NativePtr, startIndex: Int, endIndex: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __index = index
    val __value = kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.CharSequence
    val __startIndex = startIndex
    val __endIndex = endIndex
    val _result = __self.insertRange(__index, __value, __startIndex, __endIndex)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_insertRange__TypesOfArguments__Swift_Int32_ExportedKotlinPackages_kotlin_CharArray_Swift_Int32_Swift_Int32__")
public fun kotlin_text_StringBuilder_insertRange__TypesOfArguments__Swift_Int32_ExportedKotlinPackages_kotlin_CharArray_Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int, value: kotlin.native.internal.NativePtr, startIndex: Int, endIndex: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __index = index
    val __value = kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.CharArray
    val __startIndex = startIndex
    val __endIndex = endIndex
    val _result = __self.insertRange(__index, __value, __startIndex, __endIndex)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_lastIndexOf__TypesOfArguments__Swift_String__")
public fun kotlin_text_StringBuilder_lastIndexOf__TypesOfArguments__Swift_String__(self: kotlin.native.internal.NativePtr, string: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __string = interpretObjCPointer<kotlin.String>(string)
    val _result = __self.lastIndexOf(__string)
    return _result
}

@ExportedBridge("kotlin_text_StringBuilder_lastIndexOf__TypesOfArguments__Swift_String_Swift_Int32__")
public fun kotlin_text_StringBuilder_lastIndexOf__TypesOfArguments__Swift_String_Swift_Int32__(self: kotlin.native.internal.NativePtr, string: kotlin.native.internal.NativePtr, startIndex: Int): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __string = interpretObjCPointer<kotlin.String>(string)
    val __startIndex = startIndex
    val _result = __self.lastIndexOf(__string, __startIndex)
    return _result
}

@ExportedBridge("kotlin_text_StringBuilder_length_get")
public fun kotlin_text_StringBuilder_length_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val _result = __self.length
    return _result
}

@ExportedBridge("kotlin_text_StringBuilder_reverse")
public fun kotlin_text_StringBuilder_reverse(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val _result = __self.reverse()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_set__TypesOfArguments__Swift_Int32_Swift_Unicode_UTF16_CodeUnit__")
public fun kotlin_text_StringBuilder_set__TypesOfArguments__Swift_Int32_Swift_Unicode_UTF16_CodeUnit__(self: kotlin.native.internal.NativePtr, index: Int, value: Char): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __index = index
    val __value = value
    __self.`set`(__index, __value)
}

@ExportedBridge("kotlin_text_StringBuilder_setLength__TypesOfArguments__Swift_Int32__")
public fun kotlin_text_StringBuilder_setLength__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, newLength: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __newLength = newLength
    __self.setLength(__newLength)
}

@ExportedBridge("kotlin_text_StringBuilder_setRange__TypesOfArguments__Swift_Int32_Swift_Int32_Swift_String__")
public fun kotlin_text_StringBuilder_setRange__TypesOfArguments__Swift_Int32_Swift_Int32_Swift_String__(self: kotlin.native.internal.NativePtr, startIndex: Int, endIndex: Int, value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __startIndex = startIndex
    val __endIndex = endIndex
    val __value = interpretObjCPointer<kotlin.String>(value)
    val _result = __self.setRange(__startIndex, __endIndex, __value)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_subSequence__TypesOfArguments__Swift_Int32_Swift_Int32__")
public fun kotlin_text_StringBuilder_subSequence__TypesOfArguments__Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, startIndex: Int, endIndex: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __startIndex = startIndex
    val __endIndex = endIndex
    val _result = __self.subSequence(__startIndex, __endIndex)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_text_StringBuilder_substring__TypesOfArguments__Swift_Int32_Swift_Int32__")
public fun kotlin_text_StringBuilder_substring__TypesOfArguments__Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, startIndex: Int, endIndex: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __startIndex = startIndex
    val __endIndex = endIndex
    val _result = __self.substring(__startIndex, __endIndex)
    return _result.objcPtr()
}

@ExportedBridge("kotlin_text_StringBuilder_substring__TypesOfArguments__Swift_Int32__")
public fun kotlin_text_StringBuilder_substring__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, startIndex: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __startIndex = startIndex
    val _result = __self.substring(__startIndex)
    return _result.objcPtr()
}

@ExportedBridge("kotlin_text_StringBuilder_toCharArray__TypesOfArguments__ExportedKotlinPackages_kotlin_CharArray_Swift_Int32_Swift_Int32_Swift_Int32__")
public fun kotlin_text_StringBuilder_toCharArray__TypesOfArguments__ExportedKotlinPackages_kotlin_CharArray_Swift_Int32_Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, destination: kotlin.native.internal.NativePtr, destinationOffset: Int, startIndex: Int, endIndex: Int): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val __destination = kotlin.native.internal.ref.dereferenceExternalRCRef(destination) as kotlin.CharArray
    val __destinationOffset = destinationOffset
    val __startIndex = startIndex
    val __endIndex = endIndex
    __self.toCharArray(__destination, __destinationOffset, __startIndex, __endIndex)
}

@ExportedBridge("kotlin_text_StringBuilder_toString")
public fun kotlin_text_StringBuilder_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    val _result = __self.toString()
    return _result.objcPtr()
}

@ExportedBridge("kotlin_text_StringBuilder_trimToSize")
public fun kotlin_text_StringBuilder_trimToSize(self: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.text.StringBuilder
    __self.trimToSize()
}
