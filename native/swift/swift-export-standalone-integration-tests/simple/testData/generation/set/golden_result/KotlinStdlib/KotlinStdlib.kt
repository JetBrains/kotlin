@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Collection::class, "_ExportedKotlinPackages_kotlin_collections_Collection")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Iterable::class, "_ExportedKotlinPackages_kotlin_collections_Iterable")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Iterator::class, "_ExportedKotlinPackages_kotlin_collections_Iterator")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.MutableCollection::class, "_ExportedKotlinPackages_kotlin_collections_MutableCollection")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.MutableIterable::class, "_ExportedKotlinPackages_kotlin_collections_MutableIterable")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.MutableIterator::class, "_ExportedKotlinPackages_kotlin_collections_MutableIterator")

import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ExportedBridge

@ImportedBridge("KotlinStdlib_internal_functional_type_callee_anyU20ExportedKotlinPackagesU2EkotlinU2EcollectionsU2EIterator__TypesOfArguments__Swift_UnsafeMutableRawPointer__")
internal external fun KotlinStdlib_internal_functional_type_callee_anyU20ExportedKotlinPackagesU2EkotlinU2EcollectionsU2EIterator__TypesOfArguments__Swift_UnsafeMutableRawPointer__(pointerToClosure: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

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

@ExportedBridge("kotlin_collections_Iterable__TypesOfArguments__U2829202D_U20anyU20ExportedKotlinPackages_kotlin_collections_Iterator__")
public fun kotlin_collections_Iterable__TypesOfArguments__U2829202D_U20anyU20ExportedKotlinPackages_kotlin_collections_Iterator__(iterator: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __iterator = run {
        val closurePtr = iterator;
        val closureBox = interpretObjCPointer<kotlin.Any>(closurePtr).also { objc_release(closurePtr) };
        {
            val _result = KotlinStdlib_internal_functional_type_callee_anyU20ExportedKotlinPackagesU2EkotlinU2EcollectionsU2EIterator__TypesOfArguments__Swift_UnsafeMutableRawPointer__(closureBox.objcPtr())
            kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.collections.Iterator<kotlin.Any?>
        }
    }
    val _result = kotlin.collections.Iterable<kotlin.Any?>(__iterator)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
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

@ExportedBridge("kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableCollection<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = __self.add(__element)
    return _result
}

@ExportedBridge("kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableCollection<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = __self.addAll(__elements)
    return _result
}

@ExportedBridge("kotlin_collections_MutableCollection_clear")
public fun kotlin_collections_MutableCollection_clear(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableCollection<kotlin.Any?>
    __self.clear()
    return true
}

@ExportedBridge("kotlin_collections_MutableCollection_iterator")
public fun kotlin_collections_MutableCollection_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableCollection<kotlin.Any?>
    val _result = __self.iterator()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_MutableCollection_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_MutableCollection_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableCollection<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = __self.remove(__element)
    return _result
}

@ExportedBridge("kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableCollection<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = __self.removeAll(__elements)
    return _result
}

@ExportedBridge("kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableCollection<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = __self.retainAll(__elements)
    return _result
}

@ExportedBridge("kotlin_collections_MutableIterable_iterator")
public fun kotlin_collections_MutableIterable_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableIterable<kotlin.Any?>
    val _result = __self.iterator()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_MutableIterator_remove")
public fun kotlin_collections_MutableIterator_remove(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableIterator<kotlin.Any?>
    __self.remove()
    return true
}

@ExportedBridge("kotlin_collections_MutableSet_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_MutableSet_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableSet<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = __self.add(__element)
    return _result
}

@ExportedBridge("kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableSet<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = __self.addAll(__elements)
    return _result
}

@ExportedBridge("kotlin_collections_MutableSet_clear")
public fun kotlin_collections_MutableSet_clear(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableSet<kotlin.Any?>
    __self.clear()
    return true
}

@ExportedBridge("kotlin_collections_MutableSet_iterator")
public fun kotlin_collections_MutableSet_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableSet<kotlin.Any?>
    val _result = __self.iterator()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_MutableSet_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_MutableSet_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableSet<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = __self.remove(__element)
    return _result
}

@ExportedBridge("kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableSet<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = __self.removeAll(__elements)
    return _result
}

@ExportedBridge("kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.MutableSet<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = __self.retainAll(__elements)
    return _result
}

@ExportedBridge("kotlin_collections_Set_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_Set_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Set<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = __self.contains(__element)
    return _result
}

@ExportedBridge("kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Set<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = __self.containsAll(__elements)
    return _result
}

@ExportedBridge("kotlin_collections_Set_isEmpty")
public fun kotlin_collections_Set_isEmpty(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Set<kotlin.Any?>
    val _result = __self.isEmpty()
    return _result
}

@ExportedBridge("kotlin_collections_Set_iterator")
public fun kotlin_collections_Set_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Set<kotlin.Any?>
    val _result = __self.iterator()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_Set_size_get")
public fun kotlin_collections_Set_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.Set<kotlin.Any?>
    val _result = __self.size
    return _result
}

@ExportedBridge("kotlin_collections_mutableSetOf")
public fun kotlin_collections_mutableSetOf(): kotlin.native.internal.NativePtr {
    val _result = kotlin.collections.mutableSetOf<kotlin.Any?>()
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_mutableSetOf__TypesOfArguments__Swift_Array_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Vararg___")
public fun kotlin_collections_mutableSetOf__TypesOfArguments__Swift_Array_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Vararg___(elements: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __elements = interpretObjCPointer<kotlin.collections.List<kotlin.Any?>>(elements).toTypedArray()
    val _result = kotlin.collections.mutableSetOf<kotlin.Any?>(*__elements)
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_setOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_setOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(element: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = kotlin.collections.setOf<kotlin.Any?>(__element)
    return _result.objcPtr()
}

@ExportedBridge("kotlin_collections_setOf__TypesOfArguments__Swift_Array_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Vararg___")
public fun kotlin_collections_setOf__TypesOfArguments__Swift_Array_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___Vararg___(elements: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __elements = interpretObjCPointer<kotlin.collections.List<kotlin.Any?>>(elements).toTypedArray()
    val _result = kotlin.collections.setOf<kotlin.Any?>(*__elements)
    return _result.objcPtr()
}

@ExportedBridge("kotlin_collections_setOf")
public fun kotlin_collections_setOf(): kotlin.native.internal.NativePtr {
    val _result = kotlin.collections.setOf<kotlin.Any?>()
    return _result.objcPtr()
}
