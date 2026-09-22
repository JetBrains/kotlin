@file:kotlin.Suppress("DEPRECATION_ERROR")
@file:kotlin.native.internal.objc.BindClassToObjCName(MyListImpl::class, "13CollectionsV210MyListImplC")
@file:kotlin.native.internal.objc.BindClassToObjCName(MyMutableListImpl::class, "13CollectionsV217MyMutableListImplC")

import kotlin.native.internal.objc.BindReverseBridgeToMethod
import kotlin.native.internal.ImportedBridge
import kotlinx.cinterop.*
import kotlin.native.internal.ExportedBridge

@ImportedBridge("MyListImpl_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
internal external fun MyListImpl_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(MyListImpl::class, "containsAll")
public fun MyListImpl_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse(self: MyListImpl<kotlin.Any?>, elements: kotlin.collections.Collection<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __elements = kotlin.native.internal.ref.createRetainedExternalRCRef(elements)
    val _result = MyListImpl_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(__self, __elements)
    return _result
}

@ImportedBridge("MyListImpl_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun MyListImpl_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(MyListImpl::class, "contains")
public fun MyListImpl_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: MyListImpl<kotlin.Any?>, element: kotlin.Any?): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __element = if (element == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(element)
    val _result = MyListImpl_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __element)
    return _result
}

@ImportedBridge("MyListImpl_get__TypesOfArguments__Swift_Int32____reverse_swift")
internal external fun MyListImpl_get__TypesOfArguments__Swift_Int32____reverse_swift(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(MyListImpl::class, "get")
public fun MyListImpl_get__TypesOfArguments__Swift_Int32____reverse(self: MyListImpl<kotlin.Any?>, index: Int): kotlin.Any? {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = MyListImpl_get__TypesOfArguments__Swift_Int32____reverse_swift(__self, index)
    return if (_result == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.Any
}

@ImportedBridge("MyListImpl_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun MyListImpl_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(MyListImpl::class, "indexOf")
public fun MyListImpl_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: MyListImpl<kotlin.Any?>, element: kotlin.Any?): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __element = if (element == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(element)
    val _result = MyListImpl_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __element)
    return _result
}

@ImportedBridge("MyListImpl_isEmpty__reverse_swift")
internal external fun MyListImpl_isEmpty__reverse_swift(self: kotlin.native.internal.NativePtr): Boolean

@BindReverseBridgeToMethod(MyListImpl::class, "isEmpty")
public fun MyListImpl_isEmpty__reverse(self: MyListImpl<kotlin.Any?>): Boolean {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = MyListImpl_isEmpty__reverse_swift(__self)
    return _result
}

@ImportedBridge("MyListImpl_iterator__reverse_swift")
internal external fun MyListImpl_iterator__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(MyListImpl::class, "iterator")
public fun MyListImpl_iterator__reverse(self: MyListImpl<kotlin.Any?>): kotlin.collections.Iterator<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = MyListImpl_iterator__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.collections.Iterator<kotlin.Any?>
}

@ImportedBridge("MyListImpl_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
internal external fun MyListImpl_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(MyListImpl::class, "lastIndexOf")
public fun MyListImpl_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse(self: MyListImpl<kotlin.Any?>, element: kotlin.Any?): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val __element = if (element == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(element)
    val _result = MyListImpl_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(__self, __element)
    return _result
}

@ImportedBridge("MyListImpl_listIterator__TypesOfArguments__Swift_Int32____reverse_swift")
internal external fun MyListImpl_listIterator__TypesOfArguments__Swift_Int32____reverse_swift(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(MyListImpl::class, "listIterator")
public fun MyListImpl_listIterator__TypesOfArguments__Swift_Int32____reverse(self: MyListImpl<kotlin.Any?>, index: Int): kotlin.collections.ListIterator<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = MyListImpl_listIterator__TypesOfArguments__Swift_Int32____reverse_swift(__self, index)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.collections.ListIterator<kotlin.Any?>
}

@ImportedBridge("MyListImpl_listIterator__reverse_swift")
internal external fun MyListImpl_listIterator__reverse_swift(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(MyListImpl::class, "listIterator")
public fun MyListImpl_listIterator__reverse(self: MyListImpl<kotlin.Any?>): kotlin.collections.ListIterator<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = MyListImpl_listIterator__reverse_swift(__self)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.collections.ListIterator<kotlin.Any?>
}

@ImportedBridge("MyListImpl_size_get__reverse_swift")
internal external fun MyListImpl_size_get__reverse_swift(self: kotlin.native.internal.NativePtr): Int

@BindReverseBridgeToMethod(MyListImpl::class, "<get-size>")
public fun MyListImpl_size_get__reverse(self: MyListImpl<kotlin.Any?>): Int {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = MyListImpl_size_get__reverse_swift(__self)
    return _result
}

@ImportedBridge("MyListImpl_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift")
internal external fun MyListImpl_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift(self: kotlin.native.internal.NativePtr, fromIndex: Int, toIndex: Int): kotlin.native.internal.NativePtr

@BindReverseBridgeToMethod(MyListImpl::class, "subList")
public fun MyListImpl_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse(self: MyListImpl<kotlin.Any?>, fromIndex: Int, toIndex: Int): kotlin.collections.List<kotlin.Any?> {
    val __self = kotlin.native.internal.ref.createRetainedExternalRCRef(self)
    val _result = MyListImpl_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift(__self, fromIndex, toIndex)
    return kotlin.native.internal.ref.dereferenceExternalRCRef(_result) as kotlin.collections.List<kotlin.Any?>
}

@ExportedBridge("MyListImpl_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun MyListImpl_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.contains(__element) }
    return _result
}

@ExportedBridge("MyListImpl_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun MyListImpl_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.containsAll(__elements) }
    return _result
}

@ExportedBridge("MyListImpl_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection___direct", nonVirtualTargetMethod = "containsAll")
public fun MyListImpl_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection___direct(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.containsAll(__elements) }
    return _result
}

@ExportedBridge("MyListImpl_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____direct", nonVirtualTargetMethod = "contains")
public fun MyListImpl_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____direct(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.contains(__element) }
    return _result
}

@ExportedBridge("MyListImpl_get__TypesOfArguments__Swift_Int32__")
public fun MyListImpl_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val __index = index
    val _result = run { __self.`get`(__index) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("MyListImpl_get__TypesOfArguments__Swift_Int32___direct", nonVirtualTargetMethod = "get")
public fun MyListImpl_get__TypesOfArguments__Swift_Int32___direct(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val __index = index
    val _result = run { __self.`get`(__index) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("MyListImpl_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun MyListImpl_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.indexOf(__element) }
    return _result
}

@ExportedBridge("MyListImpl_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____direct", nonVirtualTargetMethod = "indexOf")
public fun MyListImpl_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____direct(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.indexOf(__element) }
    return _result
}

@ExportedBridge("MyListImpl_isEmpty")
public fun MyListImpl_isEmpty(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val _result = run { __self.isEmpty() }
    return _result
}

@ExportedBridge("MyListImpl_isEmpty_direct", nonVirtualTargetMethod = "isEmpty")
public fun MyListImpl_isEmpty_direct(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val _result = run { __self.isEmpty() }
    return _result
}

@ExportedBridge("MyListImpl_iterator")
public fun MyListImpl_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val _result = run { __self.iterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("MyListImpl_iterator_direct", nonVirtualTargetMethod = "iterator")
public fun MyListImpl_iterator_direct(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val _result = run { __self.iterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("MyListImpl_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun MyListImpl_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.lastIndexOf(__element) }
    return _result
}

@ExportedBridge("MyListImpl_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____direct", nonVirtualTargetMethod = "lastIndexOf")
public fun MyListImpl_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____direct(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.lastIndexOf(__element) }
    return _result
}

@ExportedBridge("MyListImpl_listIterator")
public fun MyListImpl_listIterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val _result = run { __self.listIterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("MyListImpl_listIterator__TypesOfArguments__Swift_Int32__")
public fun MyListImpl_listIterator__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val __index = index
    val _result = run { __self.listIterator(__index) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("MyListImpl_listIterator_direct", nonVirtualTargetMethod = "listIterator")
public fun MyListImpl_listIterator_direct(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val _result = run { __self.listIterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("MyListImpl_listIterator__TypesOfArguments__Swift_Int32___direct", nonVirtualTargetMethod = "listIterator")
public fun MyListImpl_listIterator__TypesOfArguments__Swift_Int32___direct(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val __index = index
    val _result = run { __self.listIterator(__index) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("MyListImpl_size_get")
public fun MyListImpl_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val _result = run { __self.size }
    return _result
}

@ExportedBridge("MyListImpl_size_get_direct", nonVirtualTargetMethod = "<get-size>")
public fun MyListImpl_size_get_direct(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val _result = run { __self.size }
    return _result
}

@ExportedBridge("MyListImpl_subList__TypesOfArguments__Swift_Int32_Swift_Int32__")
public fun MyListImpl_subList__TypesOfArguments__Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, fromIndex: Int, toIndex: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val __fromIndex = fromIndex
    val __toIndex = toIndex
    val _result = run { __self.subList(__fromIndex, __toIndex) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("MyListImpl_subList__TypesOfArguments__Swift_Int32_Swift_Int32___direct", nonVirtualTargetMethod = "subList")
public fun MyListImpl_subList__TypesOfArguments__Swift_Int32_Swift_Int32___direct(self: kotlin.native.internal.NativePtr, fromIndex: Int, toIndex: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyListImpl<kotlin.Any?>
    val __fromIndex = fromIndex
    val __toIndex = toIndex
    val _result = run { __self.subList(__fromIndex, __toIndex) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("MyMutableListImpl_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun MyMutableListImpl_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.add(__element) }
    return _result
}

@ExportedBridge("MyMutableListImpl_add__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun MyMutableListImpl_add__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, index: Int, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val __index = index
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.add(__index, __element) }
    return run { _result; true }
}

@ExportedBridge("MyMutableListImpl_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun MyMutableListImpl_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.addAll(__elements) }
    return _result
}

@ExportedBridge("MyMutableListImpl_addAll__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun MyMutableListImpl_addAll__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, index: Int, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val __index = index
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.addAll(__index, __elements) }
    return _result
}

@ExportedBridge("MyMutableListImpl_clear")
public fun MyMutableListImpl_clear(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val _result = run { __self.clear() }
    return run { _result; true }
}

@ExportedBridge("MyMutableListImpl_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun MyMutableListImpl_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.contains(__element) }
    return _result
}

@ExportedBridge("MyMutableListImpl_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun MyMutableListImpl_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.containsAll(__elements) }
    return _result
}

@ExportedBridge("MyMutableListImpl_get__TypesOfArguments__Swift_Int32__")
public fun MyMutableListImpl_get__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val __index = index
    val _result = run { __self.`get`(__index) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("MyMutableListImpl_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun MyMutableListImpl_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.indexOf(__element) }
    return _result
}

@ExportedBridge("MyMutableListImpl_isEmpty")
public fun MyMutableListImpl_isEmpty(self: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val _result = run { __self.isEmpty() }
    return _result
}

@ExportedBridge("MyMutableListImpl_iterator")
public fun MyMutableListImpl_iterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val _result = run { __self.iterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("MyMutableListImpl_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun MyMutableListImpl_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.lastIndexOf(__element) }
    return _result
}

@ExportedBridge("MyMutableListImpl_listIterator")
public fun MyMutableListImpl_listIterator(self: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val _result = run { __self.listIterator() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("MyMutableListImpl_listIterator__TypesOfArguments__Swift_Int32__")
public fun MyMutableListImpl_listIterator__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val __index = index
    val _result = run { __self.listIterator(__index) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("MyMutableListImpl_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun MyMutableListImpl_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, element: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.remove(__element) }
    return _result
}

@ExportedBridge("MyMutableListImpl_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun MyMutableListImpl_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.removeAll(__elements) }
    return _result
}

@ExportedBridge("MyMutableListImpl_removeAt__TypesOfArguments__Swift_Int32__")
public fun MyMutableListImpl_removeAt__TypesOfArguments__Swift_Int32__(self: kotlin.native.internal.NativePtr, index: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val __index = index
    val _result = run { __self.removeAt(__index) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("MyMutableListImpl_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__")
public fun MyMutableListImpl_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self: kotlin.native.internal.NativePtr, elements: kotlin.native.internal.NativePtr): Boolean {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val __elements = kotlin.native.internal.ref.dereferenceExternalRCRef(elements) as kotlin.collections.Collection<kotlin.Any?>
    val _result = run { __self.retainAll(__elements) }
    return _result
}

@ExportedBridge("MyMutableListImpl_set__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___")
public fun MyMutableListImpl_set__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self: kotlin.native.internal.NativePtr, index: Int, element: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val __index = index
    val __element = if (element == kotlin.native.internal.NativePtr.NULL) null else kotlin.native.internal.ref.dereferenceExternalRCRef(element) as kotlin.Any
    val _result = run { __self.`set`(__index, __element) }
    return if (_result == null) kotlin.native.internal.NativePtr.NULL else kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("MyMutableListImpl_size_get")
public fun MyMutableListImpl_size_get(self: kotlin.native.internal.NativePtr): Int {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val _result = run { __self.size }
    return _result
}

@ExportedBridge("MyMutableListImpl_subList__TypesOfArguments__Swift_Int32_Swift_Int32__")
public fun MyMutableListImpl_subList__TypesOfArguments__Swift_Int32_Swift_Int32__(self: kotlin.native.internal.NativePtr, fromIndex: Int, toIndex: Int): kotlin.native.internal.NativePtr {
    val __self = kotlin.native.internal.ref.dereferenceExternalRCRef(self) as MyMutableListImpl<kotlin.Any?>
    val __fromIndex = fromIndex
    val __toIndex = toIndex
    val _result = run { __self.subList(__fromIndex, __toIndex) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___MyListImpl_init_allocate")
public fun __root___MyListImpl_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<MyListImpl<kotlin.Any?>>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___MyListImpl_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_collections_List_Typed_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____")
public fun __root___MyListImpl_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_collections_List_Typed_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(__kt: kotlin.native.internal.NativePtr, impl: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __impl = kotlin.native.internal.ref.dereferenceExternalRCRef(impl) as kotlin.collections.List<kotlin.Any?>
    val _result = run { kotlin.native.internal.initInstance(____kt, MyListImpl<kotlin.Any?>(__impl)) }
    return run { _result; true }
}

@ExportedBridge("__root___MyMutableListImpl_init_allocate")
public fun __root___MyMutableListImpl_init_allocate(): kotlin.native.internal.NativePtr {
    val _result = run { kotlin.native.internal.createUninitializedInstance<MyMutableListImpl<kotlin.Any?>>() }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___MyMutableListImpl_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_collections_MutableList_Typed_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____")
public fun __root___MyMutableListImpl_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_collections_MutableList_Typed_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(__kt: kotlin.native.internal.NativePtr, impl: kotlin.native.internal.NativePtr): Boolean {
    val ____kt = kotlin.native.internal.ref.dereferenceExternalRCRef(__kt)!!
    val __impl = kotlin.native.internal.ref.dereferenceExternalRCRef(impl) as kotlin.collections.MutableList<kotlin.Any?>
    val _result = run { kotlin.native.internal.initInstance(____kt, MyMutableListImpl<kotlin.Any?>(__impl)) }
    return run { _result; true }
}

@ExportedBridge("__root___testMyListImplInt__TypesOfArguments__anyU20CollectionsV2_MyListImpl_Typed_Swift_Int32___")
public fun __root___testMyListImplInt__TypesOfArguments__anyU20CollectionsV2_MyListImpl_Typed_Swift_Int32___(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as MyListImpl<kotlin.Int>
    val _result = run { testMyListImplInt(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}

@ExportedBridge("__root___testMyMutableListImplInt__TypesOfArguments__anyU20CollectionsV2_MyMutableListImpl_Typed_Swift_Int32___")
public fun __root___testMyMutableListImplInt__TypesOfArguments__anyU20CollectionsV2_MyMutableListImpl_Typed_Swift_Int32___(l: kotlin.native.internal.NativePtr): kotlin.native.internal.NativePtr {
    val __l = kotlin.native.internal.ref.dereferenceExternalRCRef(l) as MyMutableListImpl<kotlin.Int>
    val _result = run { testMyMutableListImplInt(__l) }
    return kotlin.native.internal.ref.createRetainedExternalRCRef(_result)
}
