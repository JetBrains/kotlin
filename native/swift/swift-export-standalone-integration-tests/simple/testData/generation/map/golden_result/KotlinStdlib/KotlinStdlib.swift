@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

public protocol _ExportedKotlinPackages_kotlin_collections_Map_Entry<K, V>: KotlinRuntime.KotlinBase, KotlinStdlib.__ExportedKotlinPackages_kotlin_collections_Map_Entry {
    associatedtype K: KotlinRuntimeSupport._KotlinBridgeable
    associatedtype V: KotlinRuntimeSupport._KotlinBridgeable
    var key: K {
        get
    }
    var value: V {
        get
    }
}
public protocol _ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry<K, V>: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry, KotlinStdlib.__ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry {
    @discardableResult
    func setValue(
        newValue: V
    ) -> V
}
@objc(_KotlinStdlib__ExportedKotlinPackages_kotlin_collections_Map_Entry)
public protocol __ExportedKotlinPackages_kotlin_collections_Map_Entry {
}
@objc(_KotlinStdlib__ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry)
public protocol __ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry: KotlinStdlib.__ExportedKotlinPackages_kotlin_collections_Map_Entry {
}
public protocol ___ExportedKotlinPackages_kotlin_collections_Map_Entry: KotlinRuntimeSupport._KotlinBridgeable {
}
public protocol ___ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry: KotlinRuntimeSupport._KotlinBridgeable, KotlinStdlib.___ExportedKotlinPackages_kotlin_collections_Map_Entry {
}
public final class _ExportedKotlinPackages_kotlin_collections_Map_AnyEntry<K: KotlinRuntimeSupport._KotlinBridgeable, V: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry, KotlinStdlib.___ExportedKotlinPackages_kotlin_collections_Map_Entry, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
public final class _ExportedKotlinPackages_kotlin_collections_MutableMap_AnyMutableEntry<K: KotlinRuntimeSupport._KotlinBridgeable, V: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinStdlib._ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry, KotlinStdlib.___ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
    @_documentation(visibility: internal)
    public override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Collection where Self : ExportedKotlinPackages.kotlin.collections.__Collection {
    public var size: Swift.Int32 {
        get {
            return kotlin_collections_Collection_size_get(self.__externalRCRef())
        }
    }
    public func contains(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_Collection_contains__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_Collection_isEmpty(self.__externalRCRef())
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<E> {
        return { let _ptr = kotlin_collections_Collection_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func ~=(
        this: Self,
        element: E
    ) -> Swift.Bool {
        this.contains(element: element)
    }
}
extension ExportedKotlinPackages.kotlin.collections.Collection {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<E> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Iterable where Self : ExportedKotlinPackages.kotlin.collections.__Iterable {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<T> {
        return { let _ptr = kotlin_collections_Iterable_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
}
extension ExportedKotlinPackages.kotlin.collections.Iterable {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Iterator where Self : ExportedKotlinPackages.kotlin.collections.__Iterator {
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_Iterator_hasNext(self.__externalRCRef())
    }
    public func next() -> T {
        return T.__fromKotlinPointer(kotlin_collections_Iterator_next(self.__externalRCRef()))
    }
}
extension ExportedKotlinPackages.kotlin.collections.Iterator {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Map where Self : ExportedKotlinPackages.kotlin.collections.__Map {
    public var entries: Swift.Set<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_AnyEntry<K,V>> {
        get {
            return Swift.Set<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_AnyEntry<K,V>>.__fromKotlinPointer(kotlin_collections_Map_entries_get(self.__externalRCRef()))
        }
    }
    public var keys: Swift.Set<K> {
        get {
            return Swift.Set<K>.__fromKotlinPointer(kotlin_collections_Map_keys_get(self.__externalRCRef()))
        }
    }
    public var size: Swift.Int32 {
        get {
            return kotlin_collections_Map_size_get(self.__externalRCRef())
        }
    }
    public var values: any ExportedKotlinPackages.kotlin.collections.Collection<V> {
        get {
            return { let _ptr = kotlin_collections_Map_values_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<V>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<V>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
        }
    }
    public func _get(
        key: K
    ) -> V? {
        return Swift.Optional<V>.__fromKotlinPointer(kotlin_collections_Map_get__TypesOfArguments__K_Swift_Hashable__(self.__externalRCRef(), key.__retainedToKotlinPointer()))
    }
    public func containsKey(
        key: K
    ) -> Swift.Bool {
        return kotlin_collections_Map_containsKey__TypesOfArguments__K_Swift_Hashable__(self.__externalRCRef(), key.__retainedToKotlinPointer())
    }
    public func containsValue(
        value: V
    ) -> Swift.Bool {
        return kotlin_collections_Map_containsValue__TypesOfArguments__V__(self.__externalRCRef(), value.__retainedToKotlinPointer())
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_Map_isEmpty(self.__externalRCRef())
    }
    public subscript(
        key: K
    ) -> V? {
        get {
            _get(key: key)
        }
    }
}
extension ExportedKotlinPackages.kotlin.collections.Map {
    public typealias Entry<K: KotlinRuntimeSupport._KotlinBridgeable, V: KotlinRuntimeSupport._KotlinBridgeable> = KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry<K,V>
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableCollection where Self : ExportedKotlinPackages.kotlin.collections.__MutableCollection {
    @discardableResult
    public func add(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_add__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    @discardableResult
    public func addAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func clear() -> Swift.Void {
        return { kotlin_collections_MutableCollection_clear(self.__externalRCRef()); return () }()
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator<E> {
        return { let _ptr = kotlin_collections_MutableCollection_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.MutableIterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyMutableIterator<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    @discardableResult
    public func remove(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_remove__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    @discardableResult
    public func removeAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    @discardableResult
    public func retainAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
}
extension ExportedKotlinPackages.kotlin.collections.MutableCollection {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableIterable where Self : ExportedKotlinPackages.kotlin.collections.__MutableIterable {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator<T> {
        return { let _ptr = kotlin_collections_MutableIterable_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.MutableIterator<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyMutableIterator<T>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
}
extension ExportedKotlinPackages.kotlin.collections.MutableIterable {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableIterator where Self : ExportedKotlinPackages.kotlin.collections.__MutableIterator {
    public func remove() -> Swift.Void {
        return { kotlin_collections_MutableIterator_remove(self.__externalRCRef()); return () }()
    }
}
extension ExportedKotlinPackages.kotlin.collections.MutableIterator {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableMap where Self : ExportedKotlinPackages.kotlin.collections.__MutableMap {
    public var entries: any ExportedKotlinPackages.kotlin.collections.MutableSet<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_MutableMap_AnyMutableEntry<K,V>> {
        get {
            return { let _ptr = kotlin_collections_MutableMap_entries_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.MutableSet<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_MutableMap_AnyMutableEntry<K,V>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyMutableSet<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_MutableMap_AnyMutableEntry<K,V>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
        }
    }
    public var keys: any ExportedKotlinPackages.kotlin.collections.MutableSet<K> {
        get {
            return { let _ptr = kotlin_collections_MutableMap_keys_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.MutableSet<K>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyMutableSet<K>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
        }
    }
    public var values: any ExportedKotlinPackages.kotlin.collections.MutableCollection<V> {
        get {
            return { let _ptr = kotlin_collections_MutableMap_values_get(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.MutableCollection<V>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyMutableCollection<V>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
        }
    }
    public func clear() -> Swift.Void {
        return { kotlin_collections_MutableMap_clear(self.__externalRCRef()); return () }()
    }
    @discardableResult
    public func put(
        key: K,
        value: V
    ) -> V? {
        return Swift.Optional<V>.__fromKotlinPointer(kotlin_collections_MutableMap_put__TypesOfArguments__K_Swift_Hashable_V__(self.__externalRCRef(), key.__retainedToKotlinPointer(), value.__retainedToKotlinPointer()))
    }
    public func putAll(
        from: [K: V]
    ) -> Swift.Void {
        return { kotlin_collections_MutableMap_putAll__TypesOfArguments__Swift_Dictionary_K_V___(self.__externalRCRef(), from.__retainedToKotlinPointer()); return () }()
    }
    @discardableResult
    public func remove(
        key: K
    ) -> V? {
        return Swift.Optional<V>.__fromKotlinPointer(kotlin_collections_MutableMap_remove__TypesOfArguments__K_Swift_Hashable__(self.__externalRCRef(), key.__retainedToKotlinPointer()))
    }
}
extension ExportedKotlinPackages.kotlin.collections.MutableMap {
    public typealias MutableEntry<K: KotlinRuntimeSupport._KotlinBridgeable, V: KotlinRuntimeSupport._KotlinBridgeable> = KotlinStdlib._ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry<K,V>
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableSet where Self : ExportedKotlinPackages.kotlin.collections.__MutableSet {
    @discardableResult
    public func add(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_add__TypesOfArguments__E_Swift_Hashable__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    @discardableResult
    public func addAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func clear() -> Swift.Void {
        return { kotlin_collections_MutableSet_clear(self.__externalRCRef()); return () }()
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator<E> {
        return { let _ptr = kotlin_collections_MutableSet_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.MutableIterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyMutableIterator<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    @discardableResult
    public func remove(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_remove__TypesOfArguments__E_Swift_Hashable__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    @discardableResult
    public func removeAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    @discardableResult
    public func retainAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
}
extension ExportedKotlinPackages.kotlin.collections.MutableSet {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Set where Self : ExportedKotlinPackages.kotlin.collections.__Set {
    public var size: Swift.Int32 {
        get {
            return kotlin_collections_Set_size_get(self.__externalRCRef())
        }
    }
    public func contains(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_Set_contains__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_Set_isEmpty(self.__externalRCRef())
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<E> {
        return { let _ptr = kotlin_collections_Set_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func ~=(
        this: Self,
        element: E
    ) -> Swift.Bool {
        this.contains(element: element)
    }
}
extension ExportedKotlinPackages.kotlin.collections.Set {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<E> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry where Self : KotlinStdlib.___ExportedKotlinPackages_kotlin_collections_Map_Entry {
    public var key: K {
        get {
            return K.__fromKotlinPointer(kotlin_collections_Map_Entry_key_get(self.__externalRCRef()))
        }
    }
    public var value: V {
        get {
            return V.__fromKotlinPointer(kotlin_collections_Map_Entry_value_get(self.__externalRCRef()))
        }
    }
}
extension KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry {
}
@_documentation(visibility: internal)
extension KotlinStdlib._ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry where Self : KotlinStdlib.___ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry {
    @discardableResult
    public func setValue(
        newValue: V
    ) -> V {
        return V.__fromKotlinPointer(kotlin_collections_MutableMap_MutableEntry_setValue__TypesOfArguments__V__(self.__externalRCRef(), newValue.__retainedToKotlinPointer()))
    }
}
extension KotlinStdlib._ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Map {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableMap {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: KotlinStdlib.__ExportedKotlinPackages_kotlin_collections_Map_Entry {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: KotlinStdlib.__ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Collection {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableCollection {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Iterable {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableIterable {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Set {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableSet {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Iterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableIterator {
}
extension ExportedKotlinPackages.kotlin.collections {
    public protocol Collection<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections._Collection where Self.T == E {
        associatedtype E: KotlinRuntimeSupport._KotlinBridgeable
        var size: Swift.Int32 {
            get
        }
        func contains(
            element: E
        ) -> Swift.Bool
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        func isEmpty() -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<E>
    }
    public protocol Iterable<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections._Iterable {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<T>
    }
    public protocol Iterator<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections._Iterator {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        func hasNext() -> Swift.Bool
        func next() -> T
    }
    public protocol Map<K, V>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections._Map {
        associatedtype K: KotlinRuntimeSupport._KotlinBridgeable & Swift.Hashable
        associatedtype V: KotlinRuntimeSupport._KotlinBridgeable
        var entries: Swift.Set<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_AnyEntry<K,V>> {
            get
        }
        var keys: Swift.Set<K> {
            get
        }
        var size: Swift.Int32 {
            get
        }
        var values: any ExportedKotlinPackages.kotlin.collections.Collection<V> {
            get
        }
        func _get(
            key: K
        ) -> V?
        func containsKey(
            key: K
        ) -> Swift.Bool
        func containsValue(
            value: V
        ) -> Swift.Bool
        func isEmpty() -> Swift.Bool
    }
    public protocol MutableCollection<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections.MutableIterable, ExportedKotlinPackages.kotlin.collections._MutableCollection where Self.T == E {
        @discardableResult
        func add(
            element: E
        ) -> Swift.Bool
        @discardableResult
        func addAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        func clear() -> Swift.Void
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator<E>
        @discardableResult
        func remove(
            element: E
        ) -> Swift.Bool
        @discardableResult
        func removeAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        @discardableResult
        func retainAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
    }
    public protocol MutableIterable<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections._MutableIterable {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator<T>
    }
    public protocol MutableIterator<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections._MutableIterator {
        func remove() -> Swift.Void
    }
    public protocol MutableMap<K, V>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Map, ExportedKotlinPackages.kotlin.collections._MutableMap where Self.K : Swift.Hashable {
        var entries: any ExportedKotlinPackages.kotlin.collections.MutableSet<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_MutableMap_AnyMutableEntry<K,V>> {
            get
        }
        var keys: any ExportedKotlinPackages.kotlin.collections.MutableSet<K> {
            get
        }
        var values: any ExportedKotlinPackages.kotlin.collections.MutableCollection<V> {
            get
        }
        func clear() -> Swift.Void
        @discardableResult
        func put(
            key: K,
            value: V
        ) -> V?
        func putAll(
            from: [K: V]
        ) -> Swift.Void
        @discardableResult
        func remove(
            key: K
        ) -> V?
    }
    public protocol MutableSet<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Set, ExportedKotlinPackages.kotlin.collections.MutableCollection, ExportedKotlinPackages.kotlin.collections._MutableSet where Self.E : Swift.Hashable {
        @discardableResult
        func add(
            element: E
        ) -> Swift.Bool
        @discardableResult
        func addAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        func clear() -> Swift.Void
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator<E>
        @discardableResult
        func remove(
            element: E
        ) -> Swift.Bool
        @discardableResult
        func removeAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        @discardableResult
        func retainAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
    }
    public protocol Set<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections._Set {
        var size: Swift.Int32 {
            get
        }
        func contains(
            element: E
        ) -> Swift.Bool
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        func isEmpty() -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<E>
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Collection)
    public protocol _Collection: ExportedKotlinPackages.kotlin.collections._Iterable {
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Iterable)
    public protocol _Iterable {
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Iterator)
    public protocol _Iterator {
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Map)
    public protocol _Map {
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableCollection)
    public protocol _MutableCollection: ExportedKotlinPackages.kotlin.collections._Collection, ExportedKotlinPackages.kotlin.collections._MutableIterable {
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableIterable)
    public protocol _MutableIterable: ExportedKotlinPackages.kotlin.collections._Iterable {
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableIterator)
    public protocol _MutableIterator: ExportedKotlinPackages.kotlin.collections._Iterator {
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableMap)
    public protocol _MutableMap: ExportedKotlinPackages.kotlin.collections._Map {
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableSet)
    public protocol _MutableSet: ExportedKotlinPackages.kotlin.collections._Set, ExportedKotlinPackages.kotlin.collections._MutableCollection {
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_Set)
    public protocol _Set: ExportedKotlinPackages.kotlin.collections._Collection {
    }
    public protocol __Collection: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterable {
    }
    public protocol __Iterable: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol __Iterator: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol __Map: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol __MutableCollection: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Collection, ExportedKotlinPackages.kotlin.collections.__MutableIterable {
    }
    public protocol __MutableIterable: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterable {
    }
    public protocol __MutableIterator: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterator {
    }
    public protocol __MutableMap: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Map {
    }
    public protocol __MutableSet: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Set, ExportedKotlinPackages.kotlin.collections.__MutableCollection {
    }
    public protocol __Set: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Collection {
    }
    public final class AnyCollection<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections.__Collection, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        public typealias T = E
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<E> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<E>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyIterable<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections.__Iterable, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyIterator<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyMap<K: KotlinRuntimeSupport._KotlinBridgeable & Swift.Hashable, V: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Map, ExportedKotlinPackages.kotlin.collections.__Map, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyMutableCollection<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.MutableCollection, ExportedKotlinPackages.kotlin.collections.__MutableCollection, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        public typealias T = E
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyMutableIterable<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.MutableIterable, ExportedKotlinPackages.kotlin.collections.__MutableIterable, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyMutableIterator<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.MutableIterator, ExportedKotlinPackages.kotlin.collections.__MutableIterator, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<T>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyMutableMap<K: KotlinRuntimeSupport._KotlinBridgeable & Swift.Hashable, V: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.MutableMap, ExportedKotlinPackages.kotlin.collections.__MutableMap, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyMutableSet<E: KotlinRuntimeSupport._KotlinBridgeable & Swift.Hashable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.MutableSet, ExportedKotlinPackages.kotlin.collections.__MutableSet, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        public typealias T = E
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnySet<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Set, ExportedKotlinPackages.kotlin.collections.__Set, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        public typealias T = E
        @inlinable
        public var covariant: KotlinRuntimeSupport.KotlinCovariant<E> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
            }
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinCovariant<E>
        ) {
            self.init(__externalRCRefUnsafe: KotlinRuntimeSupport._cloneBridgeableRef(projection._owner.__externalRCRef()), options: .asExistentialWrapper)
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
@_cdecl("kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Collection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Collection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.containsAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Collection_contains__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_Collection_contains__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Collection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Collection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.contains(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Collection_isEmpty__reverse_swift")
package func kotlin_collections_Collection_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Collection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Collection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isEmpty()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Collection_iterator__reverse_swift")
package func kotlin_collections_Collection_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Collection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Collection>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.Iterator<__Self.E> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Collection_size_get__reverse_swift")
package func kotlin_collections_Collection_size_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Collection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Collection>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.size
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Iterable_iterator__reverse_swift")
package func kotlin_collections_Iterable_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Iterable)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Iterable>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.Iterator<__Self.T> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Iterator_hasNext__reverse_swift")
package func kotlin_collections_Iterator_hasNext__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Iterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Iterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.hasNext()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Iterator_next__reverse_swift")
package func kotlin_collections_Iterator_next__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Iterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Iterator>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.next()
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_Entry_key_get__reverse_swift")
package func kotlin_collections_Map_Entry_key_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry)
    func _open<__Self: KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.K = _self.key
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_Entry_value_get__reverse_swift")
package func kotlin_collections_Map_Entry_value_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry)
    func _open<__Self: KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_Entry>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.V = _self.value
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_containsKey__TypesOfArguments__K_Swift_Hashable____reverse_swift")
package func kotlin_collections_Map_containsKey__TypesOfArguments__K_Swift_Hashable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ key: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Map)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Map>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.containsKey(key: __Self.K.__fromKotlinPointer(key))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_containsValue__TypesOfArguments__V____reverse_swift")
package func kotlin_collections_Map_containsValue__TypesOfArguments__V____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Map)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Map>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.containsValue(value: __Self.V.__fromKotlinPointer(value))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_entries_get__reverse_swift")
package func kotlin_collections_Map_entries_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Map)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Map>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Set<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_Map_AnyEntry<__Self.K,__Self.V>> = _self.entries
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_get__TypesOfArguments__K_Swift_Hashable____reverse_swift")
package func kotlin_collections_Map_get__TypesOfArguments__K_Swift_Hashable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ key: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Map)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Map>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Optional<__Self.V> = _self._get(key: __Self.K.__fromKotlinPointer(key))
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_isEmpty__reverse_swift")
package func kotlin_collections_Map_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Map)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Map>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isEmpty()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_keys_get__reverse_swift")
package func kotlin_collections_Map_keys_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Map)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Map>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Set<__Self.K> = _self.keys
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_size_get__reverse_swift")
package func kotlin_collections_Map_size_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Map)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Map>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.size
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Map_values_get__reverse_swift")
package func kotlin_collections_Map_values_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Map)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Map>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.Collection<__Self.V> = _self.values
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableCollection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableCollection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.addAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableCollection_add__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_MutableCollection_add__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableCollection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableCollection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.add(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableCollection_clear__reverse_swift")
package func kotlin_collections_MutableCollection_clear__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableCollection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableCollection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.clear()
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableCollection_iterator__reverse_swift")
package func kotlin_collections_MutableCollection_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableCollection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableCollection>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.MutableIterator<__Self.E> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableCollection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableCollection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.removeAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableCollection_remove__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_MutableCollection_remove__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableCollection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableCollection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.remove(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableCollection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableCollection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.retainAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableIterable_iterator__reverse_swift")
package func kotlin_collections_MutableIterable_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableIterable)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableIterable>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.MutableIterator<__Self.T> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableIterator_remove__reverse_swift")
package func kotlin_collections_MutableIterator_remove__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableIterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.remove()
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableMap_MutableEntry_setValue__TypesOfArguments__V____reverse_swift")
package func kotlin_collections_MutableMap_MutableEntry_setValue__TypesOfArguments__V____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ newValue: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any KotlinStdlib._ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry)
    func _open<__Self: KotlinStdlib._ExportedKotlinPackages_kotlin_collections_MutableMap_MutableEntry>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.V = _self.setValue(newValue: __Self.V.__fromKotlinPointer(newValue))
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableMap_clear__reverse_swift")
package func kotlin_collections_MutableMap_clear__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableMap)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableMap>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.clear()
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableMap_entries_get__reverse_swift")
package func kotlin_collections_MutableMap_entries_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableMap)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableMap>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.MutableSet<KotlinStdlib._ExportedKotlinPackages_kotlin_collections_MutableMap_AnyMutableEntry<__Self.K,__Self.V>> = _self.entries
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableMap_keys_get__reverse_swift")
package func kotlin_collections_MutableMap_keys_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableMap)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableMap>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.MutableSet<__Self.K> = _self.keys
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableMap_putAll__TypesOfArguments__Swift_Dictionary_K_V_____reverse_swift")
package func kotlin_collections_MutableMap_putAll__TypesOfArguments__Swift_Dictionary_K_V_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ from: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableMap)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableMap>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.putAll(from: Swift.Dictionary<__Self.K,__Self.V>.__fromKotlinPointer(from))
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableMap_put__TypesOfArguments__K_Swift_Hashable_V____reverse_swift")
package func kotlin_collections_MutableMap_put__TypesOfArguments__K_Swift_Hashable_V____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ key: Swift.UnsafeMutableRawPointer?, _ value: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableMap)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableMap>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Optional<__Self.V> = _self.put(key: __Self.K.__fromKotlinPointer(key), value: __Self.V.__fromKotlinPointer(value))
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableMap_remove__TypesOfArguments__K_Swift_Hashable____reverse_swift")
package func kotlin_collections_MutableMap_remove__TypesOfArguments__K_Swift_Hashable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ key: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableMap)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableMap>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Optional<__Self.V> = _self.remove(key: __Self.K.__fromKotlinPointer(key))
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableMap_values_get__reverse_swift")
package func kotlin_collections_MutableMap_values_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableMap)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableMap>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.MutableCollection<__Self.V> = _self.values
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableSet)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableSet>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.addAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableSet_add__TypesOfArguments__E_Swift_Hashable____reverse_swift")
package func kotlin_collections_MutableSet_add__TypesOfArguments__E_Swift_Hashable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableSet)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableSet>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.add(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableSet_clear__reverse_swift")
package func kotlin_collections_MutableSet_clear__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableSet)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableSet>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.clear()
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableSet_iterator__reverse_swift")
package func kotlin_collections_MutableSet_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableSet)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableSet>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.MutableIterator<__Self.E> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableSet)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableSet>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.removeAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableSet_remove__TypesOfArguments__E_Swift_Hashable____reverse_swift")
package func kotlin_collections_MutableSet_remove__TypesOfArguments__E_Swift_Hashable____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableSet)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableSet>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.remove(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableSet)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableSet>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.retainAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Set)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Set>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.containsAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Set_contains__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_Set_contains__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Set)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Set>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.contains(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Set_isEmpty__reverse_swift")
package func kotlin_collections_Set_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Set)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Set>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isEmpty()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Set_iterator__reverse_swift")
package func kotlin_collections_Set_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Set)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Set>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.Iterator<__Self.E> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Set_size_get__reverse_swift")
package func kotlin_collections_Set_size_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Set)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Set>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.size
        return _result
    }
    return _open(_rawSelf)
}
