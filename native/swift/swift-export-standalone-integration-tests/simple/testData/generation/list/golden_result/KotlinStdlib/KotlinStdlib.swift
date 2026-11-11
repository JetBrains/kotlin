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
        return { let _ptr = kotlin_collections_MutableCollection_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.MutableIterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyMutableIterator<E>.__createTypedWrapper(externalRCRef: _ptr) }()
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
        return { let _ptr = kotlin_collections_MutableIterable_iterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.MutableIterator<T>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyMutableIterator<T>.__createTypedWrapper(externalRCRef: _ptr) }()
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
extension ExportedKotlinPackages.kotlin.collections.MutableList where Self : ExportedKotlinPackages.kotlin.collections.__MutableList {
    @discardableResult
    public func _set(
        index: Swift.Int32,
        element: E
    ) -> E {
        return E.__fromKotlinPointer(kotlin_collections_MutableList_set__TypesOfArguments__Swift_Int32_E__(self.__externalRCRef(), index, element.__retainedToKotlinPointer()))
    }
    @discardableResult
    public func add(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_add__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    public func add(
        index: Swift.Int32,
        element: E
    ) -> Swift.Void {
        return { kotlin_collections_MutableList_add__TypesOfArguments__Swift_Int32_E__(self.__externalRCRef(), index, element.__retainedToKotlinPointer()); return () }()
    }
    @discardableResult
    public func addAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    @discardableResult
    public func addAll(
        index: Swift.Int32,
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_addAll__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), index, elements.__externalRCRef())
    }
    public func clear() -> Swift.Void {
        return { kotlin_collections_MutableList_clear(self.__externalRCRef()); return () }()
    }
    public func listIterator() -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator<E> {
        return { let _ptr = kotlin_collections_MutableList_listIterator(self.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.MutableListIterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyMutableListIterator<E>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    public func listIterator(
        index: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator<E> {
        return { let _ptr = kotlin_collections_MutableList_listIterator__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.MutableListIterator<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyMutableListIterator<E>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
    @discardableResult
    public func remove(
        element: E
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_remove__TypesOfArguments__E__(self.__externalRCRef(), element.__retainedToKotlinPointer())
    }
    @discardableResult
    public func removeAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    @discardableResult
    public func removeAt(
        index: Swift.Int32
    ) -> E {
        return E.__fromKotlinPointer(kotlin_collections_MutableList_removeAt__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index))
    }
    @discardableResult
    public func retainAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E___(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func subList(
        fromIndex: Swift.Int32,
        toIndex: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.collections.MutableList<E> {
        return { let _ptr = kotlin_collections_MutableList_subList__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), fromIndex, toIndex); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.MutableList<E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyMutableList<E>.__createTypedWrapper(externalRCRef: _ptr) }()
    }
}
extension ExportedKotlinPackages.kotlin.collections.MutableList {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableListIterator where Self : ExportedKotlinPackages.kotlin.collections.__MutableListIterator {
    public func add(
        element: T
    ) -> Swift.Void {
        return { kotlin_collections_MutableListIterator_add__TypesOfArguments__T__(self.__externalRCRef(), element.__retainedToKotlinPointer()); return () }()
    }
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_MutableListIterator_hasNext(self.__externalRCRef())
    }
    public func next() -> T {
        return T.__fromKotlinPointer(kotlin_collections_MutableListIterator_next(self.__externalRCRef()))
    }
    public func remove() -> Swift.Void {
        return { kotlin_collections_MutableListIterator_remove(self.__externalRCRef()); return () }()
    }
    public func set(
        element: T
    ) -> Swift.Void {
        return { kotlin_collections_MutableListIterator_set__TypesOfArguments__T__(self.__externalRCRef(), element.__retainedToKotlinPointer()); return () }()
    }
}
extension ExportedKotlinPackages.kotlin.collections.MutableListIterator {
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
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._List {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableList {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Iterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableIterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._ListIterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableListIterator {
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
    public protocol MutableList<E>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.List, ExportedKotlinPackages.kotlin.collections.MutableCollection, ExportedKotlinPackages.kotlin.collections._MutableList {
        @discardableResult
        func _set(
            index: Swift.Int32,
            element: E
        ) -> E
        @discardableResult
        func add(
            element: E
        ) -> Swift.Bool
        func add(
            index: Swift.Int32,
            element: E
        ) -> Swift.Void
        @discardableResult
        func addAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        @discardableResult
        func addAll(
            index: Swift.Int32,
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        func clear() -> Swift.Void
        func listIterator() -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator<E>
        func listIterator(
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator<E>
        @discardableResult
        func remove(
            element: E
        ) -> Swift.Bool
        @discardableResult
        func removeAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        @discardableResult
        func removeAt(
            index: Swift.Int32
        ) -> E
        @discardableResult
        func retainAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection<E>
        ) -> Swift.Bool
        func subList(
            fromIndex: Swift.Int32,
            toIndex: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.collections.MutableList<E>
    }
    public protocol MutableListIterator<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.ListIterator, ExportedKotlinPackages.kotlin.collections.MutableIterator, ExportedKotlinPackages.kotlin.collections._MutableListIterator {
        func add(
            element: T
        ) -> Swift.Void
        func hasNext() -> Swift.Bool
        func next() -> T
        func remove() -> Swift.Void
        func set(
            element: T
        ) -> Swift.Void
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
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableCollection)
    public protocol _MutableCollection: ExportedKotlinPackages.kotlin.collections._Collection, ExportedKotlinPackages.kotlin.collections._MutableIterable {
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableIterable)
    public protocol _MutableIterable: ExportedKotlinPackages.kotlin.collections._Iterable {
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableIterator)
    public protocol _MutableIterator: ExportedKotlinPackages.kotlin.collections._Iterator {
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableList)
    public protocol _MutableList: ExportedKotlinPackages.kotlin.collections._List, ExportedKotlinPackages.kotlin.collections._MutableCollection {
    }
    @objc(_ExportedKotlinPackages_kotlin_collections_MutableListIterator)
    public protocol _MutableListIterator: ExportedKotlinPackages.kotlin.collections._ListIterator, ExportedKotlinPackages.kotlin.collections._MutableIterator {
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
    public protocol __MutableCollection: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Collection, ExportedKotlinPackages.kotlin.collections.__MutableIterable {
    }
    public protocol __MutableIterable: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterable {
    }
    public protocol __MutableIterator: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__Iterator {
    }
    public protocol __MutableList: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__List, ExportedKotlinPackages.kotlin.collections.__MutableCollection {
    }
    public protocol __MutableListIterator: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__ListIterator, ExportedKotlinPackages.kotlin.collections.__MutableIterator {
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
    public final class AnyMutableList<E: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.MutableList, ExportedKotlinPackages.kotlin.collections.__MutableList, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        public typealias T = E
        @_documentation(visibility: internal)
        public override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public final class AnyMutableListIterator<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.MutableListIterator, ExportedKotlinPackages.kotlin.collections.__MutableListIterator, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
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

@_cdecl("kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableCollection)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableCollection>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.addAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>.__createTypedWrapper(externalRCRef: _ptr) }())
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
        let _result: Swift.Bool = _self.removeAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>.__createTypedWrapper(externalRCRef: _ptr) }())
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
        let _result: Swift.Bool = _self.retainAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>.__createTypedWrapper(externalRCRef: _ptr) }())
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

@_cdecl("kotlin_collections_MutableListIterator_add__TypesOfArguments__T____reverse_swift")
package func kotlin_collections_MutableListIterator_add__TypesOfArguments__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableListIterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.add(element: __Self.T.__fromKotlinPointer(element))
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableListIterator_hasNext__reverse_swift")
package func kotlin_collections_MutableListIterator_hasNext__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableListIterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.hasNext()
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableListIterator_next__reverse_swift")
package func kotlin_collections_MutableListIterator_next__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableListIterator>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.T = _self.next()
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableListIterator_remove__reverse_swift")
package func kotlin_collections_MutableListIterator_remove__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableListIterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.remove()
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableListIterator_set__TypesOfArguments__T____reverse_swift")
package func kotlin_collections_MutableListIterator_set__TypesOfArguments__T____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableListIterator)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableListIterator>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.set(element: __Self.T.__fromKotlinPointer(element))
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_addAll__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableList_addAll__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.addAll(index: index, elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>.__createTypedWrapper(externalRCRef: _ptr) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableList_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.addAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>.__createTypedWrapper(externalRCRef: _ptr) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_add__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_MutableList_add__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.add(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_add__TypesOfArguments__Swift_Int32_E____reverse_swift")
package func kotlin_collections_MutableList_add__TypesOfArguments__Swift_Int32_E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.add(index: index, element: __Self.E.__fromKotlinPointer(element))
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_clear__reverse_swift")
package func kotlin_collections_MutableList_clear__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Void = _self.clear()
        return { _result; return true }()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_listIterator__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlin_collections_MutableList_listIterator__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.MutableListIterator<__Self.E> = _self.listIterator(index: index)
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_listIterator__reverse_swift")
package func kotlin_collections_MutableList_listIterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.MutableListIterator<__Self.E> = _self.listIterator()
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableList_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.removeAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>.__createTypedWrapper(externalRCRef: _ptr) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_removeAt__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlin_collections_MutableList_removeAt__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.E = _self.removeAt(index: index)
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_remove__TypesOfArguments__E____reverse_swift")
package func kotlin_collections_MutableList_remove__TypesOfArguments__E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.remove(element: __Self.E.__fromKotlinPointer(element))
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift")
package func kotlin_collections_MutableList_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection_E_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.Bool {
        let _result: Swift.Bool = _self.retainAll(elements: { let _ptr = elements; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.kotlin.collections.Collection<__Self.E>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.kotlin.collections.AnyCollection<__Self.E>.__createTypedWrapper(externalRCRef: _ptr) }())
        return _result
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_set__TypesOfArguments__Swift_Int32_E____reverse_swift")
package func kotlin_collections_MutableList_set__TypesOfArguments__Swift_Int32_E____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer? {
        let _result: __Self.E = _self._set(index: index, element: __Self.E.__fromKotlinPointer(element))
        return _result.__retainedToKotlinPointer()
    }
    return _open(_rawSelf)
}

@_cdecl("kotlin_collections_MutableList_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift")
package func kotlin_collections_MutableList_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ fromIndex: Swift.Int32, _ toIndex: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _rawSelf = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: `self`) as! (any ExportedKotlinPackages.kotlin.collections.MutableList)
    func _open<__Self: ExportedKotlinPackages.kotlin.collections.MutableList>(_ _self: __Self) -> Swift.UnsafeMutableRawPointer {
        let _result: any ExportedKotlinPackages.kotlin.collections.MutableList<__Self.E> = _self.subList(fromIndex: fromIndex, toIndex: toIndex)
        return _result.__externalRCRef()
    }
    return _open(_rawSelf)
}
