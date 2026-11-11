@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

public final class _ExportedKotlinPackages_kotlin_Enum_Companion: KotlinRuntime.KotlinBase {
    public static var shared: KotlinStdlib._ExportedKotlinPackages_kotlin_Enum_Companion {
        get {
            return KotlinStdlib._ExportedKotlinPackages_kotlin_Enum_Companion.__createClassWrapper(externalRCRef: kotlin_Enum_Companion_get())
        }
    }
    private override init() {
        fatalError()
    }
    package override init(
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
        return { let _ptr = kotlin_collections_Collection_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<E>.__createTypedWrapper(externalRCRef: _ptr) }()
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
extension ExportedKotlinPackages.kotlin.Comparable where Self : ExportedKotlinPackages.kotlin.__Comparable {
    public static func <(
        this: Self,
        other: T
    ) -> Swift.Bool {
        this._compareTo(other: other) < 0
    }
    public static func <=(
        this: Self,
        other: T
    ) -> Swift.Bool {
        this._compareTo(other: other) <= 0
    }
    public static func >(
        this: Self,
        other: T
    ) -> Swift.Bool {
        this._compareTo(other: other) > 0
    }
    public static func >=(
        this: Self,
        other: T
    ) -> Swift.Bool {
        this._compareTo(other: other) >= 0
    }
    public func _compareTo(
        other: T
    ) -> Swift.Int32 {
        return kotlin_Comparable_compareTo__TypesOfArguments__T__(self.__externalRCRef(), other.__retainedToKotlinPointer())
    }
}
extension ExportedKotlinPackages.kotlin.Comparable {
    @inlinable
    public var contravariant: KotlinRuntimeSupport.KotlinContravariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._noop)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.enums.EnumEntries where Self : ExportedKotlinPackages.kotlin.enums.__EnumEntries {
    public func sealedType() -> ExportedKotlinPackages.kotlin.enums.EnumEntries_SealedType<E> {
        switch 0 {
        default: .unknown(.init(self))
        }
    }
}
extension ExportedKotlinPackages.kotlin.enums.EnumEntries {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Iterable where Self : ExportedKotlinPackages.kotlin.collections.__Iterable {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<T> {
        return { let _ptr = kotlin_collections_Iterable_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<T>.__createTypedWrapper(externalRCRef: _ptr) }()
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
extension ExportedKotlinPackages.kotlin.collections.List where Self : ExportedKotlinPackages.kotlin.collections.__List {
    public var size: Swift.Int32 {
        get {
            return kotlin_collections_List_size_get(self.__externalRCRef())
        }
    }
    public func _get(
        index: Swift.Int32
    ) -> E {
        return E.__fromKotlinPointer(kotlin_collections_List_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index))
    }
    public func contains(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_List_contains__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_List_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func indexOf(
        element: E
    ) -> Swift.Int32 {
        return kotlin_collections_List_indexOf__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_List_isEmpty(self.__externalRCRef())
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<E> {
        return { let _ptr = kotlin_collections_List_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<E>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public func lastIndexOf(
        element: E
    ) -> Swift.Int32 {
        return kotlin_collections_List_lastIndexOf__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    public func listIterator() -> any ExportedKotlinPackages.kotlin.collections.ListIterator<E> {
        return { let _ptr = kotlin_collections_List_listIterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.ListIterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyListIterator<E>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public func listIterator(
        index: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.collections.ListIterator<E> {
        return { let _ptr = kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.ListIterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyListIterator<E>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public func subList(
        fromIndex: Swift.Int32,
        toIndex: Swift.Int32
    ) -> [E] {
        return Swift.Array<E>.__fromKotlinPointer(kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), fromIndex, toIndex))
    }
    public static func ~=(
        this: Self,
        element: E
    ) -> Swift.Bool {
        this.contains(element: element)
    }
    public subscript(
        index: Swift.Int32
    ) -> E {
        get {
            _get(index: index)
        }
    }
}
extension ExportedKotlinPackages.kotlin.collections.List {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<E> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.ListIterator where Self : ExportedKotlinPackages.kotlin.collections.__ListIterator {
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_ListIterator_hasNext(self.__externalRCRef())
    }
    public func hasPrevious() -> Swift.Bool {
        return kotlin_collections_ListIterator_hasPrevious(self.__externalRCRef())
    }
    public func next() -> T {
        return T.__fromKotlinPointer(kotlin_collections_ListIterator_next(self.__externalRCRef()))
    }
    public func nextIndex() -> Swift.Int32 {
        return kotlin_collections_ListIterator_nextIndex(self.__externalRCRef())
    }
    public func previous() -> T {
        return T.__fromKotlinPointer(kotlin_collections_ListIterator_previous(self.__externalRCRef()))
    }
    public func previousIndex() -> Swift.Int32 {
        return kotlin_collections_ListIterator_previousIndex(self.__externalRCRef())
    }
}
extension ExportedKotlinPackages.kotlin.collections.ListIterator {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Collection {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Iterable {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._List {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.enums._EnumEntries {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin._Comparable {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Iterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._ListIterator {
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
    public protocol List<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections._List {
        var size: Swift.Int32 {
            get
        }
        func _get(
            index: Swift.Int32
        ) -> E
        func contains(
            element: E
        ) -> Swift.Bool
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        func indexOf(
            element: E
        ) -> Swift.Int32
        func isEmpty() -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<E>
        func lastIndexOf(
            element: E
        ) -> Swift.Int32
        func listIterator() -> any ExportedKotlinPackages.kotlin.collections.ListIterator<E>
        func listIterator(
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.collections.ListIterator<E>
        func subList(
            fromIndex: Swift.Int32,
            toIndex: Swift.Int32
        ) -> [E]
    }
    public protocol ListIterator<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections._ListIterator {
        func hasNext() -> Swift.Bool
        func hasPrevious() -> Swift.Bool
        func next() -> T
        func nextIndex() -> Swift.Int32
        func previous() -> T
        func previousIndex() -> Swift.Int32
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
    @objc(_ExportedKotlinPackages_kotlin_collections_List)
    public protocol _List: ExportedKotlinPackages.kotlin.collections._Collection {
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_ListIterator)
    public protocol _ListIterator: ExportedKotlinPackages.kotlin.collections._Iterator {
    }
    public protocol __Collection: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterable {
    }
    public protocol __Iterable: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol __Iterator: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public protocol __List: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Collection {
    }
    public protocol __ListIterator: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterator {
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
    public final class AnyList<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.List, ExportedKotlinPackages.kotlin.collections.__List, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
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
    public final class AnyListIterator<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.ListIterator, ExportedKotlinPackages.kotlin.collections.__ListIterator, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
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
}
extension ExportedKotlinPackages.kotlin.enums {
    @frozen
    public enum EnumEntries_SealedType<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntimeSupport.SealedType {
        case unknown(ExportedKotlinPackages.kotlin.enums.EnumEntries_SealedType<E>.Unknown)
        public struct Unknown: KotlinRuntimeSupport.SealedType {
            public let value: ExportedKotlinPackages.kotlin.enums.AnyEnumEntries<E>
            init(
                _ value: KotlinRuntime.KotlinBase
            ) {
                self.value = (value as? ExportedKotlinPackages.kotlin.enums.AnyEnumEntries<E>) ?? ExportedKotlinPackages.kotlin.enums.AnyEnumEntries<E>.__createTypedWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(value.__externalRCRef()))
            }
        }
        public var value: ExportedKotlinPackages.kotlin.enums.AnyEnumEntries<E> {
            get {
                switch self {
                case let .unknown(type): type.value
                }
            }
        }
    }
    public protocol EnumEntries<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.List, ExportedKotlinPackages.kotlin.enums._EnumEntries {
        func sealedType() -> ExportedKotlinPackages.kotlin.enums.EnumEntries_SealedType<E>
    }
    @objc(_ExportedKotlinPackages_kotlin_enums_EnumEntries)
    public protocol _EnumEntries: ExportedKotlinPackages.kotlin.collections._List {
    }
    public protocol __EnumEntries: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__List {
    }
    public final class AnyEnumEntries<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.enums.EnumEntries, ExportedKotlinPackages.kotlin.enums.__EnumEntries, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        public typealias T = E
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
extension ExportedKotlinPackages.kotlin {
    public protocol Comparable<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin._Comparable {
        associatedtype T: KotlinRuntimeSupport._KotlinBridgeable
        func _compareTo(
            other: T
        ) -> Swift.Int32
    }
    @objc(_ExportedKotlinPackages_kotlin_Comparable)
    public protocol _Comparable {
    }
    @_documentation(visibility: internal)
    public protocol _Enum_ReverseDispatch<E>: KotlinRuntime.KotlinBase {
        associatedtype E: KotlinRuntimeSupport._KotlinBridgeable
    }
    public protocol __Comparable: KotlinRuntimeSupport._KotlinBridgeable {
    }
    public final class AnyComparable<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var contravariant: KotlinRuntimeSupport.KotlinContravariant<T> {
            get {
                return (_owner: self, _variance: KotlinRuntimeSupport._noop)
            }
        }
        @inlinable
        public convenience init(
            _ projection: KotlinRuntimeSupport.KotlinContravariant<T>
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
    public final class Array<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        @inlinable
        public var size: Swift.Int32 {
            get {
                return _cbridge_kotlin_Array_size_get(self.__externalRCRef())
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> T
        ) {
            fatalError()
        }
        @inlinable
        public func _get(
            index: Swift.Int32
        ) -> T {
            return T.__fromKotlinPointer(_cbridge_kotlin_Array_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index))
        }
        @inlinable
        public func _set(
            index: Swift.Int32,
            value: T
        ) -> Swift.Void {
            return { _cbridge_kotlin_Array_set__TypesOfArguments__Swift_Int32_T__(self.__externalRCRef(), index, value.__retainedToKotlinPointer()); return () }()
        }
        @inlinable
        public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator<T> {
            return { let _ptr = _cbridge_kotlin_Array_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Iterator<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyIterator<T>.__createTypedWrapper(externalRCRef: _ptr) }()
        }
        @inlinable
        public subscript(
            index: Swift.Int32
        ) -> T {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    open class Enum<E: KotlinRuntimeSupport._KotlinBridgeable>: ExportedKotlinPackages.kotlin._Enum_Base, KotlinRuntimeSupport._KotlinCustomPointerBridgeable, ExportedKotlinPackages.kotlin._Enum_ReverseDispatch, ExportedKotlinPackages.kotlin.Comparable, ExportedKotlinPackages.kotlin.__Comparable {
        public typealias Companion = KotlinStdlib._ExportedKotlinPackages_kotlin_Enum_Companion
        public typealias T = E
        @inlinable
        public final var name: Swift.String {
            get {
                return _cbridge_kotlin_Enum_name_get(self.__externalRCRef())
            }
        }
        @inlinable
        public final var ordinal: Swift.Int32 {
            get {
                return _cbridge_kotlin_Enum_ordinal_get(self.__externalRCRef())
            }
        }
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public init(
            name: Swift.String,
            ordinal: Swift.Int32
        ) {
            precondition(Self.self != ExportedKotlinPackages.kotlin.Enum<E>.self, "ExportedKotlinPackages.kotlin.Enum is an abstract class and cannot be instantiated directly")
            let __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { kotlin_Enum_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_Swift_String_Swift_Int32__(__kt, name, ordinal); return () }()
        }
        @inlinable
        public static func <(
            this: ExportedKotlinPackages.kotlin.Enum<E>,
            other: E
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        @inlinable
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Enum<E>,
            other: E
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        @inlinable
        public static func >(
            this: ExportedKotlinPackages.kotlin.Enum<E>,
            other: E
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        @inlinable
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Enum<E>,
            other: E
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        @inlinable
        public final func _compareTo(
            other: E
        ) -> Swift.Int32 {
            return _cbridge_kotlin_Enum_compareTo__TypesOfArguments__E__(self.__externalRCRef(), other.__retainedToKotlinPointer())
        }
    }
    @_documentation(visibility: internal)
    open class _Enum_Base: KotlinRuntime.KotlinBase {
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
}
@_cdecl("kotlin_Comparable_compareTo__TypesOfArguments__T____reverse_swift")
package func kotlin_Comparable_compareTo__TypesOfArguments__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ other: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.Comparable)
    func _open<__Self: ExportedKotlinPackages.kotlin.Comparable>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self._compareTo(other: __Self.T.__fromKotlinPointer(other))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.Collection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.Collection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.containsAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>.__createTypedWrapper(externalRCRef: _ptr) }())
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

@_cdecl("kotlin_collections_ListIterator_hasNext__reverse_swift")
package func kotlin_collections_ListIterator_hasNext__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.ListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.ListIterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.hasNext()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_ListIterator_hasPrevious__reverse_swift")
package func kotlin_collections_ListIterator_hasPrevious__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.ListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.ListIterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.hasPrevious()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_ListIterator_nextIndex__reverse_swift")
package func kotlin_collections_ListIterator_nextIndex__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.ListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.ListIterator>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.nextIndex()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_ListIterator_next__reverse_swift")
package func kotlin_collections_ListIterator_next__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.ListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.ListIterator>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.next()
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_ListIterator_previousIndex__reverse_swift")
package func kotlin_collections_ListIterator_previousIndex__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.ListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.ListIterator>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.previousIndex()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_ListIterator_previous__reverse_swift")
package func kotlin_collections_ListIterator_previous__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.ListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.ListIterator>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.previous()
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_List_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.containsAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>.__createTypedWrapper(externalRCRef: _ptr) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_contains__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_List_contains__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.contains(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_get__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlin_collections_List_get__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.E = _self._get(index: index)
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_indexOf__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_List_indexOf__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.indexOf(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_isEmpty__reverse_swift")
package func kotlin_collections_List_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.isEmpty()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_iterator__reverse_swift")
package func kotlin_collections_List_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.Iterator<__Self.E> = _self.iterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_lastIndexOf__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_List_lastIndexOf__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.lastIndexOf(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.ListIterator<__Self.E> = _self.listIterator(index: index)
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_listIterator__reverse_swift")
package func kotlin_collections_List_listIterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.ListIterator<__Self.E> = _self.listIterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_size_get__reverse_swift")
package func kotlin_collections_List_size_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.Int32 {
        let _result: Swift.Int32 = _self.size
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift")
package func kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ fromIndex: Swift.Int32, _ toIndex: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.List)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.List>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: Swift.Array<__Self.E> = _self.subList(fromIndex: fromIndex, toIndex: toIndex)
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Array_get__TypesOfArguments__Swift_Int32__(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    return kotlin_Array_get__TypesOfArguments__Swift_Int32__(_p0, _p1)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Array_iterator(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    return kotlin_Array_iterator(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Array_set__TypesOfArguments__Swift_Int32_T__(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.Int32, _ _p2: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    return kotlin_Array_set__TypesOfArguments__Swift_Int32_T__(_p0, _p1, _p2)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Array_size_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    return kotlin_Array_size_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Enum_compareTo__TypesOfArguments__E__(_ _p0: Swift.UnsafeMutableRawPointer, _ _p1: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    return kotlin_Enum_compareTo__TypesOfArguments__E__(_p0, _p1)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Enum_name_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.String {
    return kotlin_Enum_name_get(_p0)
}

@usableFromInline
@discardableResult
internal func _cbridge_kotlin_Enum_ordinal_get(_ _p0: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    return kotlin_Enum_ordinal_get(_p0)
}
