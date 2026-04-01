@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.kotlin.collections.Collection where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var size: Swift.Int32 {
        get {
            return kotlin_collections_Collection_size_get(self.__externalRCRef())
        }
    }
    public func contains(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_Collection_isEmpty(self.__externalRCRef())
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_Collection_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public static func ~=(
        this: Self,
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this.contains(element: element)
    }
}
extension ExportedKotlinPackages.kotlin.collections.Collection {
}
extension ExportedKotlinPackages.kotlin.collections.Iterable where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_Iterable_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
}
extension ExportedKotlinPackages.kotlin.collections.Iterable {
}
extension ExportedKotlinPackages.kotlin.collections.Iterator where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_Iterator_hasNext(self.__externalRCRef())
    }
    public func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_Iterator_next(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
}
extension ExportedKotlinPackages.kotlin.collections.Iterator {
}
extension ExportedKotlinPackages.kotlin.collections.List where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public var size: Swift.Int32 {
        get {
            return kotlin_collections_List_size_get(self.__externalRCRef())
        }
    }
    public func _get(
        index: Swift.Int32
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_List_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func contains(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_List_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func indexOf(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Int32 {
        return kotlin_collections_List_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_List_isEmpty(self.__externalRCRef())
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_List_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public func lastIndexOf(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Int32 {
        return kotlin_collections_List_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func listIterator() -> any ExportedKotlinPackages.kotlin.collections.ListIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_List_listIterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
    }
    public func listIterator(
        index: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.collections.ListIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index)) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
    }
    public func subList(
        fromIndex: Swift.Int32,
        toIndex: Swift.Int32
    ) -> [(any KotlinRuntimeSupport._KotlinBridgeable)?] {
        return kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), fromIndex, toIndex) as! Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    }
    public static func ~=(
        this: Self,
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this.contains(element: element)
    }
    public subscript(
        index: Swift.Int32
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get {
            _get(index: index)
        }
    }
}
extension ExportedKotlinPackages.kotlin.collections.List {
}
extension ExportedKotlinPackages.kotlin.collections.ListIterator where Self : KotlinRuntimeSupport._KotlinBridgeable {
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_ListIterator_hasNext(self.__externalRCRef())
    }
    public func hasPrevious() -> Swift.Bool {
        return kotlin_collections_ListIterator_hasPrevious(self.__externalRCRef())
    }
    public func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_ListIterator_next(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func nextIndex() -> Swift.Int32 {
        return kotlin_collections_ListIterator_nextIndex(self.__externalRCRef())
    }
    public func previous() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_ListIterator_previous(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func previousIndex() -> Swift.Int32 {
        return kotlin_collections_ListIterator_previousIndex(self.__externalRCRef())
    }
}
extension ExportedKotlinPackages.kotlin.collections.ListIterator {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.List where Wrapped : ExportedKotlinPackages.kotlin.collections._List {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Collection where Wrapped : ExportedKotlinPackages.kotlin.collections._Collection {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Iterator where Wrapped : ExportedKotlinPackages.kotlin.collections._Iterator {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.ListIterator where Wrapped : ExportedKotlinPackages.kotlin.collections._ListIterator {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Iterable where Wrapped : ExportedKotlinPackages.kotlin.collections._Iterable {
}
extension ExportedKotlinPackages.kotlin.collections {
    public protocol Collection: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable {
        var size: Swift.Int32 {
            get
        }
        func contains(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func isEmpty() -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public protocol Iterable: KotlinRuntime.KotlinBase {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public protocol Iterator: KotlinRuntime.KotlinBase {
        func hasNext() -> Swift.Bool
        func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    }
    public protocol List: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection {
        var size: Swift.Int32 {
            get
        }
        func _get(
            index: Swift.Int32
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func contains(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func indexOf(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Int32
        func isEmpty() -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
        func lastIndexOf(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Int32
        func listIterator() -> any ExportedKotlinPackages.kotlin.collections.ListIterator
        func listIterator(
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.collections.ListIterator
        func subList(
            fromIndex: Swift.Int32,
            toIndex: Swift.Int32
        ) -> [(any KotlinRuntimeSupport._KotlinBridgeable)?]
    }
    public protocol ListIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator {
        func hasNext() -> Swift.Bool
        func hasPrevious() -> Swift.Bool
        func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func nextIndex() -> Swift.Int32
        func previous() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func previousIndex() -> Swift.Int32
    }
    @objc(_Collection)
    package protocol _Collection: ExportedKotlinPackages.kotlin.collections._Iterable {
    }
    @objc(_Iterable)
    package protocol _Iterable {
    }
    @objc(_Iterator)
    package protocol _Iterator {
    }
    @objc(_List)
    package protocol _List: ExportedKotlinPackages.kotlin.collections._Collection {
    }
    @objc(_ListIterator)
    package protocol _ListIterator: ExportedKotlinPackages.kotlin.collections._Iterator {
    }
}
