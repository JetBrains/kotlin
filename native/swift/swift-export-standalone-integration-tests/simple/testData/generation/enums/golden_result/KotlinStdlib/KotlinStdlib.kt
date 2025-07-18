@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Array::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE5ArrayC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Enum::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE4EnumC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.Enum.Companion::class, "22ExportedKotlinPackages6kotlinO12KotlinStdlibE4EnumC9CompanionC")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Collection::class, "_Collection")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Iterable::class, "_Iterable")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Iterator::class, "_Iterator")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.List::class, "_List")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.ListIterator::class, "_ListIterator")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.enums.EnumEntries::class, "_EnumEntries")

import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.*
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ExportedBridge("kotlin_Array_get__TypesOfArguments__Swift_Int32__")
public fun kotlin_Array_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Array<kotlin.Any?>
    val __index = index
    val _result = __self.`get`(__index)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_Array_iterator")
public fun kotlin_Array_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Array<kotlin.Any?>
    val _result = __self.iterator()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_Array_set__TypesOfArguments__Swift_Int32_Swift_Optional_KotlinRuntime_KotlinBase___")
public fun kotlin_Array_set__TypesOfArguments__Swift_Int32_Swift_Optional_KotlinRuntime_KotlinBase___(self: kotlin.native.internal.NativePtr, index: Int, value: kotlin.native.internal.NativePtr): Unit {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Array<kotlin.Any?>
    val __index = index
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Any
    __self.`set`(__index, __value)
}

@ExportedBridge("kotlin_Array_size_get")
public fun kotlin_Array_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Array<kotlin.Any?>
    val _result = __self.size
    return _result
}

@ExportedBridge("kotlin_Enum_Companion_get")
public fun kotlin_Enum_Companion_get(): kotlin.native.internal.NativePtr {
    val _result = kotlin.Enum.Companion
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_Enum_compareTo__TypesOfArguments__ExportedKotlinPackages_kotlin_Enum__")
public fun kotlin_Enum_compareTo__TypesOfArguments__ExportedKotlinPackages_kotlin_Enum__(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Enum<kotlin.Enum<*>>
    val __other = kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Enum<kotlin.Enum<*>>
    val _result = __self.compareTo(__other)
    return _result
}

@ExportedBridge("kotlin_Enum_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___")
public fun kotlin_Enum_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self: kotlin.native.internal.NativePtr, other: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Enum<kotlin.Enum<*>>
    val __other = if (other == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(other) as kotlin.Any
    val _result = __self.equals(__other)
    return _result
}

@ExportedBridge("kotlin_Enum_hashCode")
public fun kotlin_Enum_hashCode(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Enum<kotlin.Enum<*>>
    val _result = __self.hashCode()
    return _result
}

@ExportedBridge("kotlin_Enum_name_get")
public fun kotlin_Enum_name_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Enum<kotlin.Enum<*>>
    val _result = __self.name
    return _result.objcPtr()
}

@ExportedBridge("kotlin_Enum_ordinal_get")
public fun kotlin_Enum_ordinal_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Enum<kotlin.Enum<*>>
    val _result = __self.ordinal
    return _result
}

@ExportedBridge("kotlin_Enum_toString")
public fun kotlin_Enum_toString(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.Enum<kotlin.Enum<*>>
    val _result = __self.toString()
    return _result.objcPtr()
}

@ExportedBridge("kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___")
public fun kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Collection<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = __self.contains(__element)
    return _result
}

@ExportedBridge("kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Collection<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = __self.containsAll(__elements)
    return _result
}

@ExportedBridge("kotlin_collections_Collection_isEmpty")
public fun kotlin_collections_Collection_isEmpty(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Collection<kotlin.Any?>
    val _result = __self.isEmpty()
    return _result
}

@ExportedBridge("kotlin_collections_Collection_iterator")
public fun kotlin_collections_Collection_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Collection<kotlin.Any?>
    val _result = __self.iterator()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_Collection_size_get")
public fun kotlin_collections_Collection_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Collection<kotlin.Any?>
    val _result = __self.size
    return _result
}

@ExportedBridge("kotlin_collections_Iterable_iterator")
public fun kotlin_collections_Iterable_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Iterable<kotlin.Any?>
    val _result = __self.iterator()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_Iterator_hasNext")
public fun kotlin_collections_Iterator_hasNext(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Iterator<kotlin.Any?>
    val _result = __self.hasNext()
    return _result
}

@ExportedBridge("kotlin_collections_Iterator_next")
public fun kotlin_collections_Iterator_next(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Iterator<kotlin.Any?>
    val _result = __self.next()
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_ListIterator_hasNext")
public fun kotlin_collections_ListIterator_hasNext(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = __self.hasNext()
    return _result
}

@ExportedBridge("kotlin_collections_ListIterator_hasPrevious")
public fun kotlin_collections_ListIterator_hasPrevious(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = __self.hasPrevious()
    return _result
}

@ExportedBridge("kotlin_collections_ListIterator_next")
public fun kotlin_collections_ListIterator_next(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = __self.next()
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_ListIterator_nextIndex")
public fun kotlin_collections_ListIterator_nextIndex(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = __self.nextIndex()
    return _result
}

@ExportedBridge("kotlin_collections_ListIterator_previous")
public fun kotlin_collections_ListIterator_previous(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = __self.previous()
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_ListIterator_previousIndex")
public fun kotlin_collections_ListIterator_previousIndex(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = __self.previousIndex()
    return _result
}

@ExportedBridge("kotlin_collections_List_contains__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___")
public fun kotlin_collections_List_contains__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = __self.contains(__element)
    return _result
}

@ExportedBridge("kotlin_collections_List_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun kotlin_collections_List_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = __self.containsAll(__elements)
    return _result
}

@ExportedBridge("kotlin_collections_List_get__TypesOfArguments__Swift_Int32__")
public fun kotlin_collections_List_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val __index = index
    val _result = __self.`get`(__index)
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_List_indexOf__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___")
public fun kotlin_collections_List_indexOf__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = __self.indexOf(__element)
    return _result
}

@ExportedBridge("kotlin_collections_List_isEmpty")
public fun kotlin_collections_List_isEmpty(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val _result = __self.isEmpty()
    return _result
}

@ExportedBridge("kotlin_collections_List_iterator")
public fun kotlin_collections_List_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val _result = __self.iterator()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_List_lastIndexOf__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___")
public fun kotlin_collections_List_lastIndexOf__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = __self.lastIndexOf(__element)
    return _result
}

@ExportedBridge("kotlin_collections_List_listIterator")
public fun kotlin_collections_List_listIterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val _result = __self.listIterator()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32__")
public fun kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val __index = index
    val _result = __self.listIterator(__index)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_List_size_get")
public fun kotlin_collections_List_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val _result = __self.size
    return _result
}

@ExportedBridge("kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32__")
public fun kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, fromIndex: Int, toIndex: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val __fromIndex = fromIndex
    val __toIndex = toIndex
    val _result = __self.subList(__fromIndex, __toIndex)
    return _result.objcPtr()
}
