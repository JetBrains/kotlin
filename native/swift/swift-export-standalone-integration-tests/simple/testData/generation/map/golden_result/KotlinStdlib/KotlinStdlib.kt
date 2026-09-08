@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Collection::class, "_ExportedKotlinPackages_kotlin_collections_Collection")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Iterable::class, "_ExportedKotlinPackages_kotlin_collections_Iterable")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Iterator::class, "_ExportedKotlinPackages_kotlin_collections_Iterator")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.MutableCollection::class, "_ExportedKotlinPackages_kotlin_collections_MutableCollection")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.MutableIterable::class, "_ExportedKotlinPackages_kotlin_collections_MutableIterable")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.MutableIterator::class, "_ExportedKotlinPackages_kotlin_collections_MutableIterator")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Map.Entry::class, "_KotlinStdlib__ExportedKotlinPackages_kotlin_collections_Map_Entry")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.MutableMap.MutableEntry::class, "_KotlinStdlib__ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ImportedBridge("kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
internal external fun kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.Collection::class, "containsAll")
public fun kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse(self: kotlin.collections.Collection<kotlin.Any?>, elements: kotlin.collections.Collection<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __elements = kotlin.native.internal.ref.createRetainedExternalRCRef(elements)
    val _result = kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(__self, __elements)
    return _result
}

@ImportedBridge("kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.Collection::class, "contains")
public fun kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlin.collections.Collection<kotlin.Any?>, element: kotlin.Any?): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __element = if (element == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(element)
    val _result = kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __element)
    return _result
}

@ImportedBridge("kotlin_collections_Collection_isEmpty__reverse_swift")
internal external fun kotlin_collections_Collection_isEmpty__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.Collection::class, "isEmpty")
public fun kotlin_collections_Collection_isEmpty__reverse(self: kotlin.collections.Collection<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_Collection_isEmpty__reverse_swift(__self)
    return _result
}

@ImportedBridge("kotlin_collections_Collection_iterator__reverse_swift")
internal external fun kotlin_collections_Collection_iterator__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.Collection::class, "iterator")
public fun kotlin_collections_Collection_iterator__reverse(self: kotlin.collections.Collection<kotlin.Any?>): kotlin.collections.Iterator<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_Collection_iterator__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.collections.Iterator<kotlin.Any?>
}

@ImportedBridge("kotlin_collections_Collection_size_get__reverse_swift")
internal external fun kotlin_collections_Collection_size_get__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(kotlin.collections.Collection::class, "<get-size>")
public fun kotlin_collections_Collection_size_get__reverse(self: kotlin.collections.Collection<kotlin.Any?>): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_Collection_size_get__reverse_swift(__self)
    return _result
}

@ImportedBridge("kotlin_collections_Iterable_iterator__reverse_swift")
internal external fun kotlin_collections_Iterable_iterator__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.Iterable::class, "iterator")
public fun kotlin_collections_Iterable_iterator__reverse(self: kotlin.collections.Iterable<kotlin.Any?>): kotlin.collections.Iterator<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_Iterable_iterator__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.collections.Iterator<kotlin.Any?>
}

@ImportedBridge("kotlin_collections_Iterator_hasNext__reverse_swift")
internal external fun kotlin_collections_Iterator_hasNext__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.Iterator::class, "hasNext")
public fun kotlin_collections_Iterator_hasNext__reverse(self: kotlin.collections.Iterator<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_Iterator_hasNext__reverse_swift(__self)
    return _result
}

@ImportedBridge("kotlin_collections_Iterator_next__reverse_swift")
internal external fun kotlin_collections_Iterator_next__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.Iterator::class, "next")
public fun kotlin_collections_Iterator_next__reverse(self: kotlin.collections.Iterator<kotlin.Any?>): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_Iterator_next__reverse_swift(__self)
    return if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.Any
}

@ImportedBridge("kotlin_collections_Map_Entry_key_get__reverse_swift")
internal external fun kotlin_collections_Map_Entry_key_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.Map.Entry::class, "<get-key>")
public fun kotlin_collections_Map_Entry_key_get__reverse(self: kotlin.collections.Map.Entry<kotlin.Any?, kotlin.Any?>): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_Map_Entry_key_get__reverse_swift(__self)
    return if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.Any
}

@ImportedBridge("kotlin_collections_Map_Entry_value_get__reverse_swift")
internal external fun kotlin_collections_Map_Entry_value_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.Map.Entry::class, "<get-value>")
public fun kotlin_collections_Map_Entry_value_get__reverse(self: kotlin.collections.Map.Entry<kotlin.Any?, kotlin.Any?>): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_Map_Entry_value_get__reverse_swift(__self)
    return if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.Any
}

@ImportedBridge("kotlin_collections_Map_containsKey__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlin_collections_Map_containsKey__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, key: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.Map::class, "containsKey")
public fun kotlin_collections_Map_containsKey__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlin.collections.Map<kotlin.Any?, kotlin.Any?>, key: kotlin.Any?): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __key = if (key == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(key)
    val _result = kotlin_collections_Map_containsKey__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __key)
    return _result
}

@ImportedBridge("kotlin_collections_Map_containsValue__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlin_collections_Map_containsValue__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.Map::class, "containsValue")
public fun kotlin_collections_Map_containsValue__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlin.collections.Map<kotlin.Any?, kotlin.Any?>, value: kotlin.Any?): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __value = if (value == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(value)
    val _result = kotlin_collections_Map_containsValue__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __value)
    return _result
}

@ImportedBridge("kotlin_collections_Map_get__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlin_collections_Map_get__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, key: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.Map::class, "get")
public fun kotlin_collections_Map_get__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlin.collections.Map<kotlin.Any?, kotlin.Any?>, key: kotlin.Any?): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __key = if (key == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(key)
    val _result = kotlin_collections_Map_get__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __key)
    return if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.Any
}

@ImportedBridge("kotlin_collections_Map_isEmpty__reverse_swift")
internal external fun kotlin_collections_Map_isEmpty__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.Map::class, "isEmpty")
public fun kotlin_collections_Map_isEmpty__reverse(self: kotlin.collections.Map<kotlin.Any?, kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_Map_isEmpty__reverse_swift(__self)
    return _result
}

@ImportedBridge("kotlin_collections_Map_keys_get__reverse_swift")
internal external fun kotlin_collections_Map_keys_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.Map::class, "<get-keys>")
public fun kotlin_collections_Map_keys_get__reverse(self: kotlin.collections.Map<kotlin.Any?, kotlin.Any?>): kotlin.collections.Set<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_Map_keys_get__reverse_swift(__self)
    return interpretObjCPointer<kotlin.collections.Set<kotlin.Any?>>(_result)
}

@ImportedBridge("kotlin_collections_Map_size_get__reverse_swift")
internal external fun kotlin_collections_Map_size_get__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(kotlin.collections.Map::class, "<get-size>")
public fun kotlin_collections_Map_size_get__reverse(self: kotlin.collections.Map<kotlin.Any?, kotlin.Any?>): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_Map_size_get__reverse_swift(__self)
    return _result
}

@ImportedBridge("kotlin_collections_Map_values_get__reverse_swift")
internal external fun kotlin_collections_Map_values_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.Map::class, "<get-values>")
public fun kotlin_collections_Map_values_get__reverse(self: kotlin.collections.Map<kotlin.Any?, kotlin.Any?>): kotlin.collections.Collection<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_Map_values_get__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.collections.Collection<kotlin.Any?>
}

@ImportedBridge("kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
internal external fun kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.MutableCollection::class, "addAll")
public fun kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse(self: kotlin.collections.MutableCollection<kotlin.Any?>, elements: kotlin.collections.Collection<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __elements = kotlin.native.internal.ref.createRetainedExternalRCRef(elements)
    val _result = kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(__self, __elements)
    return _result
}

@ImportedBridge("kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.MutableCollection::class, "add")
public fun kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlin.collections.MutableCollection<kotlin.Any?>, element: kotlin.Any?): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __element = if (element == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(element)
    val _result = kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __element)
    return _result
}

@ImportedBridge("kotlin_collections_MutableCollection_clear__reverse_swift")
internal external fun kotlin_collections_MutableCollection_clear__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.MutableCollection::class, "clear")
public fun kotlin_collections_MutableCollection_clear__reverse(self: kotlin.collections.MutableCollection<kotlin.Any?>): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_MutableCollection_clear__reverse_swift(__self)
    return run<Unit> { _result }
}

@ImportedBridge("kotlin_collections_MutableCollection_iterator__reverse_swift")
internal external fun kotlin_collections_MutableCollection_iterator__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.MutableCollection::class, "iterator")
public fun kotlin_collections_MutableCollection_iterator__reverse(self: kotlin.collections.MutableCollection<kotlin.Any?>): kotlin.collections.MutableIterator<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_MutableCollection_iterator__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.collections.MutableIterator<kotlin.Any?>
}

@ImportedBridge("kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
internal external fun kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.MutableCollection::class, "removeAll")
public fun kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse(self: kotlin.collections.MutableCollection<kotlin.Any?>, elements: kotlin.collections.Collection<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __elements = kotlin.native.internal.ref.createRetainedExternalRCRef(elements)
    val _result = kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(__self, __elements)
    return _result
}

@ImportedBridge("kotlin_collections_MutableCollection_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlin_collections_MutableCollection_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.MutableCollection::class, "remove")
public fun kotlin_collections_MutableCollection_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlin.collections.MutableCollection<kotlin.Any?>, element: kotlin.Any?): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __element = if (element == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(element)
    val _result = kotlin_collections_MutableCollection_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __element)
    return _result
}

@ImportedBridge("kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
internal external fun kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.MutableCollection::class, "retainAll")
public fun kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse(self: kotlin.collections.MutableCollection<kotlin.Any?>, elements: kotlin.collections.Collection<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __elements = kotlin.native.internal.ref.createRetainedExternalRCRef(elements)
    val _result = kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(__self, __elements)
    return _result
}

@ImportedBridge("kotlin_collections_MutableIterable_iterator__reverse_swift")
internal external fun kotlin_collections_MutableIterable_iterator__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.MutableIterable::class, "iterator")
public fun kotlin_collections_MutableIterable_iterator__reverse(self: kotlin.collections.MutableIterable<kotlin.Any?>): kotlin.collections.MutableIterator<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_MutableIterable_iterator__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.collections.MutableIterator<kotlin.Any?>
}

@ImportedBridge("kotlin_collections_MutableIterator_remove__reverse_swift")
internal external fun kotlin_collections_MutableIterator_remove__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.MutableIterator::class, "remove")
public fun kotlin_collections_MutableIterator_remove__reverse(self: kotlin.collections.MutableIterator<kotlin.Any?>): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_MutableIterator_remove__reverse_swift(__self)
    return run<Unit> { _result }
}

@ImportedBridge("kotlin_collections_MutableMap_MutableEntry_setValue__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlin_collections_MutableMap_MutableEntry_setValue__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.MutableMap.MutableEntry::class, "setValue")
public fun kotlin_collections_MutableMap_MutableEntry_setValue__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlin.collections.MutableMap.MutableEntry<kotlin.Any?, kotlin.Any?>, newValue: kotlin.Any?): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __newValue = if (newValue == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(newValue)
    val _result = kotlin_collections_MutableMap_MutableEntry_setValue__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __newValue)
    return if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.Any
}

@ImportedBridge("kotlin_collections_MutableMap_clear__reverse_swift")
internal external fun kotlin_collections_MutableMap_clear__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.MutableMap::class, "clear")
public fun kotlin_collections_MutableMap_clear__reverse(self: kotlin.collections.MutableMap<kotlin.Any?, kotlin.Any?>): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_MutableMap_clear__reverse_swift(__self)
    return run<Unit> { _result }
}

@ImportedBridge("kotlin_collections_MutableMap_entries_get__reverse_swift")
internal external fun kotlin_collections_MutableMap_entries_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.MutableMap::class, "<get-entries>")
public fun kotlin_collections_MutableMap_entries_get__reverse(self: kotlin.collections.MutableMap<kotlin.Any?, kotlin.Any?>): kotlin.collections.MutableSet<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_MutableMap_entries_get__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.collections.MutableSet<kotlin.Any?>
}

@ImportedBridge("kotlin_collections_MutableMap_keys_get__reverse_swift")
internal external fun kotlin_collections_MutableMap_keys_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.MutableMap::class, "<get-keys>")
public fun kotlin_collections_MutableMap_keys_get__reverse(self: kotlin.collections.MutableMap<kotlin.Any?, kotlin.Any?>): kotlin.collections.MutableSet<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_MutableMap_keys_get__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.collections.MutableSet<kotlin.Any?>
}

@ImportedBridge("kotlin_collections_MutableMap_putAll__TypesOfArguments__Swift_Dictionary_Swift_Optional_Swift_AnyHashable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable______reverse_swift")
internal external fun kotlin_collections_MutableMap_putAll__TypesOfArguments__Swift_Dictionary_Swift_Optional_Swift_AnyHashable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable______reverse_swift(self: kotlin.native.internal.NativePtr, from: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.MutableMap::class, "putAll")
public fun kotlin_collections_MutableMap_putAll__TypesOfArguments__Swift_Dictionary_Swift_Optional_Swift_AnyHashable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable______reverse(self: kotlin.collections.MutableMap<kotlin.Any?, kotlin.Any?>, from: kotlin.collections.Map<kotlin.Any?, kotlin.Any?>): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __from = from.objcPtr()
    val _result = kotlin_collections_MutableMap_putAll__TypesOfArguments__Swift_Dictionary_Swift_Optional_Swift_AnyHashable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable______reverse_swift(__self, __from)
    return run<Unit> { _result }
}

@ImportedBridge("kotlin_collections_MutableMap_put__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlin_collections_MutableMap_put__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, key: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.MutableMap::class, "put")
public fun kotlin_collections_MutableMap_put__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlin.collections.MutableMap<kotlin.Any?, kotlin.Any?>, key: kotlin.Any?, value: kotlin.Any?): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __key = if (key == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(key)
    val __value = if (value == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(value)
    val _result = kotlin_collections_MutableMap_put__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __key, __value)
    return if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.Any
}

@ImportedBridge("kotlin_collections_MutableMap_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlin_collections_MutableMap_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, key: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.MutableMap::class, "remove")
public fun kotlin_collections_MutableMap_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlin.collections.MutableMap<kotlin.Any?, kotlin.Any?>, key: kotlin.Any?): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __key = if (key == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(key)
    val _result = kotlin_collections_MutableMap_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __key)
    return if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.Any
}

@ImportedBridge("kotlin_collections_MutableMap_values_get__reverse_swift")
internal external fun kotlin_collections_MutableMap_values_get__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.MutableMap::class, "<get-values>")
public fun kotlin_collections_MutableMap_values_get__reverse(self: kotlin.collections.MutableMap<kotlin.Any?, kotlin.Any?>): kotlin.collections.MutableCollection<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_MutableMap_values_get__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.collections.MutableCollection<kotlin.Any?>
}

@ImportedBridge("kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
internal external fun kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.MutableSet::class, "addAll")
public fun kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse(self: kotlin.collections.MutableSet<kotlin.Any?>, elements: kotlin.collections.Collection<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __elements = kotlin.native.internal.ref.createRetainedExternalRCRef(elements)
    val _result = kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(__self, __elements)
    return _result
}

@ImportedBridge("kotlin_collections_MutableSet_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlin_collections_MutableSet_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.MutableSet::class, "add")
public fun kotlin_collections_MutableSet_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlin.collections.MutableSet<kotlin.Any?>, element: kotlin.Any?): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __element = if (element == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(element)
    val _result = kotlin_collections_MutableSet_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __element)
    return _result
}

@ImportedBridge("kotlin_collections_MutableSet_clear__reverse_swift")
internal external fun kotlin_collections_MutableSet_clear__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.MutableSet::class, "clear")
public fun kotlin_collections_MutableSet_clear__reverse(self: kotlin.collections.MutableSet<kotlin.Any?>): Unit {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_MutableSet_clear__reverse_swift(__self)
    return run<Unit> { _result }
}

@ImportedBridge("kotlin_collections_MutableSet_iterator__reverse_swift")
internal external fun kotlin_collections_MutableSet_iterator__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.MutableSet::class, "iterator")
public fun kotlin_collections_MutableSet_iterator__reverse(self: kotlin.collections.MutableSet<kotlin.Any?>): kotlin.collections.MutableIterator<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_MutableSet_iterator__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.collections.MutableIterator<kotlin.Any?>
}

@ImportedBridge("kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
internal external fun kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.MutableSet::class, "removeAll")
public fun kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse(self: kotlin.collections.MutableSet<kotlin.Any?>, elements: kotlin.collections.Collection<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __elements = kotlin.native.internal.ref.createRetainedExternalRCRef(elements)
    val _result = kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(__self, __elements)
    return _result
}

@ImportedBridge("kotlin_collections_MutableSet_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlin_collections_MutableSet_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.MutableSet::class, "remove")
public fun kotlin_collections_MutableSet_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlin.collections.MutableSet<kotlin.Any?>, element: kotlin.Any?): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __element = if (element == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(element)
    val _result = kotlin_collections_MutableSet_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __element)
    return _result
}

@ImportedBridge("kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
internal external fun kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.MutableSet::class, "retainAll")
public fun kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse(self: kotlin.collections.MutableSet<kotlin.Any?>, elements: kotlin.collections.Collection<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __elements = kotlin.native.internal.ref.createRetainedExternalRCRef(elements)
    val _result = kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(__self, __elements)
    return _result
}

@ImportedBridge("kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
internal external fun kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.Set::class, "containsAll")
public fun kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse(self: kotlin.collections.Set<kotlin.Any?>, elements: kotlin.collections.Collection<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __elements = kotlin.native.internal.ref.createRetainedExternalRCRef(elements)
    val _result = kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(__self, __elements)
    return _result
}

@ImportedBridge("kotlin_collections_Set_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlin_collections_Set_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.Set::class, "contains")
public fun kotlin_collections_Set_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlin.collections.Set<kotlin.Any?>, element: kotlin.Any?): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __element = if (element == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(element)
    val _result = kotlin_collections_Set_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __element)
    return _result
}

@ImportedBridge("kotlin_collections_Set_isEmpty__reverse_swift")
internal external fun kotlin_collections_Set_isEmpty__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.Set::class, "isEmpty")
public fun kotlin_collections_Set_isEmpty__reverse(self: kotlin.collections.Set<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_Set_isEmpty__reverse_swift(__self)
    return _result
}

@ImportedBridge("kotlin_collections_Set_iterator__reverse_swift")
internal external fun kotlin_collections_Set_iterator__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.Set::class, "iterator")
public fun kotlin_collections_Set_iterator__reverse(self: kotlin.collections.Set<kotlin.Any?>): kotlin.collections.Iterator<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_Set_iterator__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.collections.Iterator<kotlin.Any?>
}

@ImportedBridge("kotlin_collections_Set_size_get__reverse_swift")
internal external fun kotlin_collections_Set_size_get__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(kotlin.collections.Set::class, "<get-size>")
public fun kotlin_collections_Set_size_get__reverse(self: kotlin.collections.Set<kotlin.Any?>): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = kotlin_collections_Set_size_get__reverse_swift(__self)
    return _result
}

@ExportedBridge("kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Collection<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.contains(__element) }
    return _result
}

@ExportedBridge("kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Collection<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.containsAll(__elements) }
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

@ExportedBridge("kotlin_collections_Map_Entry_key_get")
public fun kotlin_collections_Map_Entry_key_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Map.Entry<kotlin.Any?, kotlin.Any?>
    val _result = run { __self.key }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_Map_Entry_value_get")
public fun kotlin_collections_Map_Entry_value_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Map.Entry<kotlin.Any?, kotlin.Any?>
    val _result = run { __self.value }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_Map_containsKey__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_Map_containsKey__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, key: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Map<kotlin.Any?, kotlin.Any?>
    val __key = if (key == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(key) as kotlin.Any
    val _result = run { __self.containsKey(__key) }
    return _result
}

@ExportedBridge("kotlin_collections_Map_containsValue__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_Map_containsValue__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Map<kotlin.Any?, kotlin.Any?>
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Any
    val _result = run { __self.containsValue(__value) }
    return _result
}

@ExportedBridge("kotlin_collections_Map_get__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_Map_get__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, key: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Map<kotlin.Any?, kotlin.Any?>
    val __key = if (key == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(key) as kotlin.Any
    val _result = run { __self.`get`(__key) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_Map_isEmpty")
public fun kotlin_collections_Map_isEmpty(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Map<kotlin.Any?, kotlin.Any?>
    val _result = run { __self.isEmpty() }
    return _result
}

@ExportedBridge("kotlin_collections_Map_keys_get")
public fun kotlin_collections_Map_keys_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Map<kotlin.Any?, kotlin.Any?>
    val _result = run { __self.keys }
    return _result.objcPtr()
}

@ExportedBridge("kotlin_collections_Map_size_get")
public fun kotlin_collections_Map_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Map<kotlin.Any?, kotlin.Any?>
    val _result = run { __self.size }
    return _result
}

@ExportedBridge("kotlin_collections_Map_values_get")
public fun kotlin_collections_Map_values_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Map<kotlin.Any?, kotlin.Any?>
    val _result = run { __self.values }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableCollection<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.add(__element) }
    return _result
}

@ExportedBridge("kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableCollection<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.addAll(__elements) }
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

@ExportedBridge("kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableCollection<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.removeAll(__elements) }
    return _result
}

@ExportedBridge("kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableCollection<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.retainAll(__elements) }
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

@ExportedBridge("kotlin_collections_MutableMap_MutableEntry_setValue__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_MutableMap_MutableEntry_setValue__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, newValue: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableMap.MutableEntry<kotlin.Any?, kotlin.Any?>
    val __newValue = if (newValue == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(newValue) as kotlin.Any
    val _result = run { __self.setValue(__newValue) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_MutableMap_clear")
public fun kotlin_collections_MutableMap_clear(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableMap<kotlin.Any?, kotlin.Any?>
    val _result = run { __self.clear() }
    return run { _result; true }
}

@ExportedBridge("kotlin_collections_MutableMap_entries_get")
public fun kotlin_collections_MutableMap_entries_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableMap<kotlin.Any?, kotlin.Any?>
    val _result = run { __self.entries }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_MutableMap_keys_get")
public fun kotlin_collections_MutableMap_keys_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableMap<kotlin.Any?, kotlin.Any?>
    val _result = run { __self.keys }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_MutableMap_put__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_MutableMap_put__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, key: kotlin.native.internal.NativePtr, value: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableMap<kotlin.Any?, kotlin.Any?>
    val __key = if (key == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(key) as kotlin.Any
    val __value = if (value == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(value) as kotlin.Any
    val _result = run { __self.put(__key, __value) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_MutableMap_putAll__TypesOfArguments__Swift_Dictionary_Swift_Optional_Swift_AnyHashable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____")
public fun kotlin_collections_MutableMap_putAll__TypesOfArguments__Swift_Dictionary_Swift_Optional_Swift_AnyHashable__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(self: kotlin.native.internal.NativePtr, from: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableMap<kotlin.Any?, kotlin.Any?>
    val __from = interpretObjCPointer<kotlin.collections.Map<kotlin.Any?, kotlin.Any?>>(from)
    val _result = run { __self.putAll(__from) }
    return run { _result; true }
}

@ExportedBridge("kotlin_collections_MutableMap_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_MutableMap_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, key: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableMap<kotlin.Any?, kotlin.Any?>
    val __key = if (key == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(key) as kotlin.Any
    val _result = run { __self.remove(__key) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_MutableMap_values_get")
public fun kotlin_collections_MutableMap_values_get(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableMap<kotlin.Any?, kotlin.Any?>
    val _result = run { __self.values }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_MutableSet_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_MutableSet_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableSet<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.add(__element) }
    return _result
}

@ExportedBridge("kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableSet<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.addAll(__elements) }
    return _result
}

@ExportedBridge("kotlin_collections_MutableSet_clear")
public fun kotlin_collections_MutableSet_clear(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableSet<kotlin.Any?>
    val _result = run { __self.clear() }
    return run { _result; true }
}

@ExportedBridge("kotlin_collections_MutableSet_iterator")
public fun kotlin_collections_MutableSet_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableSet<kotlin.Any?>
    val _result = run { __self.iterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_MutableSet_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_MutableSet_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableSet<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.remove(__element) }
    return _result
}

@ExportedBridge("kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableSet<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.removeAll(__elements) }
    return _result
}

@ExportedBridge("kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableSet<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.retainAll(__elements) }
    return _result
}

@ExportedBridge("kotlin_collections_Set_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_Set_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Set<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.contains(__element) }
    return _result
}

@ExportedBridge("kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Set<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.containsAll(__elements) }
    return _result
}

@ExportedBridge("kotlin_collections_Set_isEmpty")
public fun kotlin_collections_Set_isEmpty(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Set<kotlin.Any?>
    val _result = run { __self.isEmpty() }
    return _result
}

@ExportedBridge("kotlin_collections_Set_iterator")
public fun kotlin_collections_Set_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Set<kotlin.Any?>
    val _result = run { __self.iterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_Set_size_get")
public fun kotlin_collections_Set_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Set<kotlin.Any?>
    val _result = run { __self.size }
    return _result
}
