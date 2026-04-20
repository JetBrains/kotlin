@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Collection::class, "_Collection")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Iterable::class, "_Iterable")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.Iterator::class, "_Iterator")
@file:kotlin.native.internal.objc.BindClassToObjCName(kotlin.collections.ListIterator::class, "_ListIterator")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge
import kotlinx.cinterop.internal.convertBlockPtrToKotlinFunction

@ImportedBridge("kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.Collection::class, "contains")
public fun kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlin.collections.Collection<kotlin.Any?>, element: kotlin.Any?): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __element = if (element == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(element)
    val __result = kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __element)
    return __result
}

@ImportedBridge("kotlin_collections_Collection_isEmpty__reverse_swift")
internal external fun kotlin_collections_Collection_isEmpty__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.Collection::class, "isEmpty")
public fun kotlin_collections_Collection_isEmpty__reverse(self: kotlin.collections.Collection<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_Collection_isEmpty__reverse_swift(__self)
    return __result
}

@ImportedBridge("kotlin_collections_Collection_iterator__reverse_swift")
internal external fun kotlin_collections_Collection_iterator__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.Collection::class, "iterator")
public fun kotlin_collections_Collection_iterator__reverse(self: kotlin.collections.Collection<kotlin.Any?>): kotlin.collections.Iterator<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_Collection_iterator__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as kotlin.collections.Iterator<kotlin.Any?>
}

@ImportedBridge("kotlin_collections_Iterable_iterator__reverse_swift")
internal external fun kotlin_collections_Iterable_iterator__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.Iterable::class, "iterator")
public fun kotlin_collections_Iterable_iterator__reverse(self: kotlin.collections.Iterable<kotlin.Any?>): kotlin.collections.Iterator<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_Iterable_iterator__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as kotlin.collections.Iterator<kotlin.Any?>
}

@ImportedBridge("kotlin_collections_Iterator_hasNext__reverse_swift")
internal external fun kotlin_collections_Iterator_hasNext__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.Iterator::class, "hasNext")
public fun kotlin_collections_Iterator_hasNext__reverse(self: kotlin.collections.Iterator<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_Iterator_hasNext__reverse_swift(__self)
    return __result
}

@ImportedBridge("kotlin_collections_Iterator_next__reverse_swift")
internal external fun kotlin_collections_Iterator_next__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.Iterator::class, "next")
public fun kotlin_collections_Iterator_next__reverse(self: kotlin.collections.Iterator<kotlin.Any?>): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_Iterator_next__reverse_swift(__self)
    return if (__result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as kotlin.Any
}

@ImportedBridge("kotlin_collections_ListIterator_hasNext__reverse_swift")
internal external fun kotlin_collections_ListIterator_hasNext__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.ListIterator::class, "hasNext")
public fun kotlin_collections_ListIterator_hasNext__reverse(self: kotlin.collections.ListIterator<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_ListIterator_hasNext__reverse_swift(__self)
    return __result
}

@ImportedBridge("kotlin_collections_ListIterator_hasPrevious__reverse_swift")
internal external fun kotlin_collections_ListIterator_hasPrevious__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.ListIterator::class, "hasPrevious")
public fun kotlin_collections_ListIterator_hasPrevious__reverse(self: kotlin.collections.ListIterator<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_ListIterator_hasPrevious__reverse_swift(__self)
    return __result
}

@ImportedBridge("kotlin_collections_ListIterator_nextIndex__reverse_swift")
internal external fun kotlin_collections_ListIterator_nextIndex__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(kotlin.collections.ListIterator::class, "nextIndex")
public fun kotlin_collections_ListIterator_nextIndex__reverse(self: kotlin.collections.ListIterator<kotlin.Any?>): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_ListIterator_nextIndex__reverse_swift(__self)
    return __result
}

@ImportedBridge("kotlin_collections_ListIterator_next__reverse_swift")
internal external fun kotlin_collections_ListIterator_next__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.ListIterator::class, "next")
public fun kotlin_collections_ListIterator_next__reverse(self: kotlin.collections.ListIterator<kotlin.Any?>): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_ListIterator_next__reverse_swift(__self)
    return if (__result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as kotlin.Any
}

@ImportedBridge("kotlin_collections_ListIterator_previousIndex__reverse_swift")
internal external fun kotlin_collections_ListIterator_previousIndex__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(kotlin.collections.ListIterator::class, "previousIndex")
public fun kotlin_collections_ListIterator_previousIndex__reverse(self: kotlin.collections.ListIterator<kotlin.Any?>): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_ListIterator_previousIndex__reverse_swift(__self)
    return __result
}

@ImportedBridge("kotlin_collections_ListIterator_previous__reverse_swift")
internal external fun kotlin_collections_ListIterator_previous__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.ListIterator::class, "previous")
public fun kotlin_collections_ListIterator_previous__reverse(self: kotlin.collections.ListIterator<kotlin.Any?>): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_ListIterator_previous__reverse_swift(__self)
    return if (__result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as kotlin.Any
}

@ImportedBridge("kotlin_collections_List_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlin_collections_List_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.List::class, "contains")
public fun kotlin_collections_List_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlin.collections.List<kotlin.Any?>, element: kotlin.Any?): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __element = if (element == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(element)
    val __result = kotlin_collections_List_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __element)
    return __result
}

@ImportedBridge("kotlin_collections_List_get__TypesOfArguments__Swift_Int32____reverse_swift")
internal external fun kotlin_collections_List_get__TypesOfArguments__Swift_Int32____reverse_swift(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.List::class, "get")
public fun kotlin_collections_List_get__TypesOfArguments__Swift_Int32____reverse(self: kotlin.collections.List<kotlin.Any?>, index: Int): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_List_get__TypesOfArguments__Swift_Int32____reverse_swift(__self, index)
    return if (__result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as kotlin.Any
}

@ImportedBridge("kotlin_collections_List_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlin_collections_List_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(kotlin.collections.List::class, "indexOf")
public fun kotlin_collections_List_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlin.collections.List<kotlin.Any?>, element: kotlin.Any?): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __element = if (element == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(element)
    val __result = kotlin_collections_List_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __element)
    return __result
}

@ImportedBridge("kotlin_collections_List_isEmpty__reverse_swift")
internal external fun kotlin_collections_List_isEmpty__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(kotlin.collections.List::class, "isEmpty")
public fun kotlin_collections_List_isEmpty__reverse(self: kotlin.collections.List<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_List_isEmpty__reverse_swift(__self)
    return __result
}

@ImportedBridge("kotlin_collections_List_iterator__reverse_swift")
internal external fun kotlin_collections_List_iterator__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.List::class, "iterator")
public fun kotlin_collections_List_iterator__reverse(self: kotlin.collections.List<kotlin.Any?>): kotlin.collections.Iterator<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_List_iterator__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as kotlin.collections.Iterator<kotlin.Any?>
}

@ImportedBridge("kotlin_collections_List_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun kotlin_collections_List_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(kotlin.collections.List::class, "lastIndexOf")
public fun kotlin_collections_List_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: kotlin.collections.List<kotlin.Any?>, element: kotlin.Any?): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __element = if (element == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(element)
    val __result = kotlin_collections_List_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __element)
    return __result
}

@ImportedBridge("kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32____reverse_swift")
internal external fun kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32____reverse_swift(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.List::class, "listIterator")
public fun kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32____reverse(self: kotlin.collections.List<kotlin.Any?>, index: Int): kotlin.collections.ListIterator<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32____reverse_swift(__self, index)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as kotlin.collections.ListIterator<kotlin.Any?>
}

@ImportedBridge("kotlin_collections_List_listIterator__reverse_swift")
internal external fun kotlin_collections_List_listIterator__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.List::class, "listIterator")
public fun kotlin_collections_List_listIterator__reverse(self: kotlin.collections.List<kotlin.Any?>): kotlin.collections.ListIterator<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_List_listIterator__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(__result) as kotlin.collections.ListIterator<kotlin.Any?>
}

@ImportedBridge("kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift")
internal external fun kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift(self: kotlin.native.internal.NativePtr, fromIndex: Int, toIndex: Int): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(kotlin.collections.List::class, "subList")
public fun kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse(self: kotlin.collections.List<kotlin.Any?>, fromIndex: Int, toIndex: Int): kotlin.collections.List<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __result = kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift(__self, fromIndex, toIndex)
    return interpretObjCPointer<kotlin.collections.List<kotlin.Any?>>(__result)
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

@ExportedBridge("kotlin_collections_ListIterator_hasNext")
public fun kotlin_collections_ListIterator_hasNext(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = run { __self.hasNext() }
    return _result
}

@ExportedBridge("kotlin_collections_ListIterator_hasPrevious")
public fun kotlin_collections_ListIterator_hasPrevious(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = run { __self.hasPrevious() }
    return _result
}

@ExportedBridge("kotlin_collections_ListIterator_next")
public fun kotlin_collections_ListIterator_next(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = run { __self.next() }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_ListIterator_nextIndex")
public fun kotlin_collections_ListIterator_nextIndex(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = run { __self.nextIndex() }
    return _result
}

@ExportedBridge("kotlin_collections_ListIterator_previous")
public fun kotlin_collections_ListIterator_previous(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = run { __self.previous() }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_ListIterator_previousIndex")
public fun kotlin_collections_ListIterator_previousIndex(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.ListIterator<kotlin.Any?>
    val _result = run { __self.previousIndex() }
    return _result
}

@ExportedBridge("kotlin_collections_List_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_List_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.contains(__element) }
    return _result
}

@ExportedBridge("kotlin_collections_List_get__TypesOfArguments__Swift_Int32__")
public fun kotlin_collections_List_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val __index = index
    val _result = run { __self.`get`(__index) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_List_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_List_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.indexOf(__element) }
    return _result
}

@ExportedBridge("kotlin_collections_List_isEmpty")
public fun kotlin_collections_List_isEmpty(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val _result = run { __self.isEmpty() }
    return _result
}

@ExportedBridge("kotlin_collections_List_iterator")
public fun kotlin_collections_List_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val _result = run { __self.iterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_List_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun kotlin_collections_List_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.lastIndexOf(__element) }
    return _result
}

@ExportedBridge("kotlin_collections_List_listIterator")
public fun kotlin_collections_List_listIterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val _result = run { __self.listIterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32__")
public fun kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val __index = index
    val _result = run { __self.listIterator(__index) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("kotlin_collections_List_size_get")
public fun kotlin_collections_List_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val _result = run { __self.size }
    return _result
}

@ExportedBridge("kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32__")
public fun kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, fromIndex: Int, toIndex: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as kotlin.collections.List<kotlin.Any?>
    val __fromIndex = fromIndex
    val __toIndex = toIndex
    val _result = run { __self.subList(__fromIndex, __toIndex) }
    return _result.objcPtr()
}
