@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

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
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableCollection)
    public protocol _MutableCollection: ExportedKotlinPackages.kotlin.collections._Collection, ExportedKotlinPackages.kotlin.collections._MutableIterable {
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableIterable)
    public protocol _MutableIterable: ExportedKotlinPackages.kotlin.collections._Iterable {
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableIterator)
    public protocol _MutableIterator: ExportedKotlinPackages.kotlin.collections._Iterator {
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
    public protocol __MutableCollection: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Collection, ExportedKotlinPackages.kotlin.collections.__MutableIterable {
    }
    public protocol __MutableIterable: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterable {
    }
    public protocol __MutableIterator: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterator {
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
