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
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_Collection_isEmpty(self.__externalRCRef())
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_Collection_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.Iterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterator
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
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Iterable where Self : ExportedKotlinPackages.kotlin.collections.__Iterable {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_Iterable_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.Iterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
}
extension ExportedKotlinPackages.kotlin.collections.Iterable {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.Iterator where Self : ExportedKotlinPackages.kotlin.collections.__Iterator {
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_Iterator_hasNext(self.__externalRCRef())
    }
    public func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_Iterator_next(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
}
extension ExportedKotlinPackages.kotlin.collections.Iterator {
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
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_List_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func contains(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_List_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_List_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
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
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_List_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.Iterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public func lastIndexOf(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Int32 {
        return kotlin_collections_List_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func listIterator() -> any ExportedKotlinPackages.kotlin.collections.ListIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_List_listIterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
    }
    public func listIterator(
        index: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.collections.ListIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index), conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
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
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.ListIterator where Self : ExportedKotlinPackages.kotlin.collections.__ListIterator {
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_ListIterator_hasNext(self.__externalRCRef())
    }
    public func hasPrevious() -> Swift.Bool {
        return kotlin_collections_ListIterator_hasPrevious(self.__externalRCRef())
    }
    public func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_ListIterator_next(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func nextIndex() -> Swift.Int32 {
        return kotlin_collections_ListIterator_nextIndex(self.__externalRCRef())
    }
    public func previous() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_ListIterator_previous(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func previousIndex() -> Swift.Int32 {
        return kotlin_collections_ListIterator_previousIndex(self.__externalRCRef())
    }
}
extension ExportedKotlinPackages.kotlin.collections.ListIterator {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableCollection where Self : ExportedKotlinPackages.kotlin.collections.__MutableCollection {
    public func add(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func addAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func clear() -> Swift.Void {
        return { kotlin_collections_MutableCollection_clear(self.__externalRCRef()); return () }()
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableCollection_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableIterator
    }
    public func remove(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func removeAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func retainAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
}
extension ExportedKotlinPackages.kotlin.collections.MutableCollection {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableIterable where Self : ExportedKotlinPackages.kotlin.collections.__MutableIterable {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableIterable_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableIterator
    }
}
extension ExportedKotlinPackages.kotlin.collections.MutableIterable {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableIterator where Self : ExportedKotlinPackages.kotlin.collections.__MutableIterator {
    public func remove() -> Swift.Void {
        return { kotlin_collections_MutableIterator_remove(self.__externalRCRef()); return () }()
    }
}
extension ExportedKotlinPackages.kotlin.collections.MutableIterator {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableList where Self : ExportedKotlinPackages.kotlin.collections.__MutableList {
    public func _set(
        index: Swift.Int32,
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_MutableList_set__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), index, element.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func add(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func add(
        index: Swift.Int32,
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlin_collections_MutableList_add__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), index, element.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    public func addAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func addAll(
        index: Swift.Int32,
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_addAll__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), index, elements.__externalRCRef())
    }
    public func clear() -> Swift.Void {
        return { kotlin_collections_MutableList_clear(self.__externalRCRef()); return () }()
    }
    public func listIterator() -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableList_listIterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableListIterator
    }
    public func listIterator(
        index: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableList_listIterator__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableListIterator
    }
    public func remove(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func removeAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func removeAt(
        index: Swift.Int32
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_MutableList_removeAt__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func retainAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableList_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func subList(
        fromIndex: Swift.Int32,
        toIndex: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.collections.MutableList {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableList_subList__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), fromIndex, toIndex), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    }
}
extension ExportedKotlinPackages.kotlin.collections.MutableList {
}
@_documentation(visibility: internal)
extension ExportedKotlinPackages.kotlin.collections.MutableListIterator where Self : ExportedKotlinPackages.kotlin.collections.__MutableListIterator {
    public func add(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlin_collections_MutableListIterator_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_MutableListIterator_hasNext(self.__externalRCRef())
    }
    public func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch kotlin_collections_MutableListIterator_next(self.__externalRCRef()) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func remove() -> Swift.Void {
        return { kotlin_collections_MutableListIterator_remove(self.__externalRCRef()); return () }()
    }
    public func set(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { kotlin_collections_MutableListIterator_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
}
extension ExportedKotlinPackages.kotlin.collections.MutableListIterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.List, ExportedKotlinPackages.kotlin.collections.__List where Wrapped : ExportedKotlinPackages.kotlin.collections._List {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableList, ExportedKotlinPackages.kotlin.collections.__MutableList where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableList {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections.__Collection where Wrapped : ExportedKotlinPackages.kotlin.collections._Collection {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableCollection, ExportedKotlinPackages.kotlin.collections.__MutableCollection where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableCollection {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator where Wrapped : ExportedKotlinPackages.kotlin.collections._Iterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.ListIterator, ExportedKotlinPackages.kotlin.collections.__ListIterator where Wrapped : ExportedKotlinPackages.kotlin.collections._ListIterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableListIterator, ExportedKotlinPackages.kotlin.collections.__MutableListIterator where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableListIterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections.__Iterable where Wrapped : ExportedKotlinPackages.kotlin.collections._Iterable {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableIterable, ExportedKotlinPackages.kotlin.collections.__MutableIterable where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableIterable {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableIterator, ExportedKotlinPackages.kotlin.collections.__MutableIterator where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableIterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._List {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableList {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Collection {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableCollection {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Iterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._ListIterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableListIterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Iterable {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableIterable {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableIterator {
}
extension ExportedKotlinPackages.kotlin.collections {
    public protocol Collection: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections._Collection {
        var size: Swift.Int32 {
            get
        }
        func contains(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func isEmpty() -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public protocol Iterable: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections._Iterable {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public protocol Iterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections._Iterator {
        func hasNext() -> Swift.Bool
        func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
    }
    public protocol List: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections._List {
        var size: Swift.Int32 {
            get
        }
        func _get(
            index: Swift.Int32
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func contains(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
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
    public protocol ListIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections._ListIterator {
        func hasNext() -> Swift.Bool
        func hasPrevious() -> Swift.Bool
        func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func nextIndex() -> Swift.Int32
        func previous() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func previousIndex() -> Swift.Int32
    }
    public protocol MutableCollection: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections.MutableIterable, ExportedKotlinPackages.kotlin.collections._MutableCollection {
        func add(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func addAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func clear() -> Swift.Void
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator
        func remove(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func removeAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func retainAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
    }
    public protocol MutableIterable: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections._MutableIterable {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator
    }
    public protocol MutableIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections._MutableIterator {
        func remove() -> Swift.Void
    }
    public protocol MutableList: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.List, ExportedKotlinPackages.kotlin.collections.MutableCollection, ExportedKotlinPackages.kotlin.collections._MutableList {
        func _set(
            index: Swift.Int32,
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func add(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func add(
            index: Swift.Int32,
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void
        func addAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func addAll(
            index: Swift.Int32,
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func clear() -> Swift.Void
        func listIterator() -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator
        func listIterator(
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator
        func remove(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Bool
        func removeAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func removeAt(
            index: Swift.Int32
        ) -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func retainAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func subList(
            fromIndex: Swift.Int32,
            toIndex: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.collections.MutableList
    }
    public protocol MutableListIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.ListIterator, ExportedKotlinPackages.kotlin.collections.MutableIterator, ExportedKotlinPackages.kotlin.collections._MutableListIterator {
        func add(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
        ) -> Swift.Void
        func hasNext() -> Swift.Bool
        func next() -> (any KotlinRuntimeSupport._KotlinBridgeable)?
        func remove() -> Swift.Void
        func set(
            element: (any KotlinRuntimeSupport._KotlinBridgeable)?
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
}
@_cdecl("kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_Collection_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection
    let _result: Swift.Bool = _self.containsAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection
    let _result: Swift.Bool = _self.contains(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_Collection_isEmpty__reverse_swift")
package func kotlin_collections_Collection_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection
    let _result: Swift.Bool = _self.isEmpty()
    return _result
}

@_cdecl("kotlin_collections_Collection_iterator__reverse_swift")
package func kotlin_collections_Collection_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection
    let _result: any ExportedKotlinPackages.kotlin.collections.Iterator = _self.iterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_Collection_size_get__reverse_swift")
package func kotlin_collections_Collection_size_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection
    let _result: Swift.Int32 = _self.size
    return _result
}

@_cdecl("kotlin_collections_Iterable_iterator__reverse_swift")
package func kotlin_collections_Iterable_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Iterable.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterable
    let _result: any ExportedKotlinPackages.kotlin.collections.Iterator = _self.iterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_Iterator_hasNext__reverse_swift")
package func kotlin_collections_Iterator_hasNext__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Iterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    let _result: Swift.Bool = _self.hasNext()
    return _result
}

@_cdecl("kotlin_collections_Iterator_next__reverse_swift")
package func kotlin_collections_Iterator_next__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Iterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.next()
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_collections_ListIterator_hasNext__reverse_swift")
package func kotlin_collections_ListIterator_hasNext__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
    let _result: Swift.Bool = _self.hasNext()
    return _result
}

@_cdecl("kotlin_collections_ListIterator_hasPrevious__reverse_swift")
package func kotlin_collections_ListIterator_hasPrevious__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
    let _result: Swift.Bool = _self.hasPrevious()
    return _result
}

@_cdecl("kotlin_collections_ListIterator_nextIndex__reverse_swift")
package func kotlin_collections_ListIterator_nextIndex__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
    let _result: Swift.Int32 = _self.nextIndex()
    return _result
}

@_cdecl("kotlin_collections_ListIterator_next__reverse_swift")
package func kotlin_collections_ListIterator_next__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.next()
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_collections_ListIterator_previousIndex__reverse_swift")
package func kotlin_collections_ListIterator_previousIndex__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
    let _result: Swift.Int32 = _self.previousIndex()
    return _result
}

@_cdecl("kotlin_collections_ListIterator_previous__reverse_swift")
package func kotlin_collections_ListIterator_previous__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.previous()
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_collections_List_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_List_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: Swift.Bool = _self.containsAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_List_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_List_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: Swift.Bool = _self.contains(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_List_get__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlin_collections_List_get__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self._get(index: index)
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_collections_List_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_List_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: Swift.Int32 = _self.indexOf(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_List_isEmpty__reverse_swift")
package func kotlin_collections_List_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: Swift.Bool = _self.isEmpty()
    return _result
}

@_cdecl("kotlin_collections_List_iterator__reverse_swift")
package func kotlin_collections_List_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: any ExportedKotlinPackages.kotlin.collections.Iterator = _self.iterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_List_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_List_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: Swift.Int32 = _self.lastIndexOf(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlin_collections_List_listIterator__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: any ExportedKotlinPackages.kotlin.collections.ListIterator = _self.listIterator(index: index)
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_List_listIterator__reverse_swift")
package func kotlin_collections_List_listIterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: any ExportedKotlinPackages.kotlin.collections.ListIterator = _self.listIterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_List_size_get__reverse_swift")
package func kotlin_collections_List_size_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: Swift.Int32 = _self.size
    return _result
}

@_cdecl("kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift")
package func kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ fromIndex: Swift.Int32, _ toIndex: Swift.Int32) -> Any {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
    let _result: Swift.Array<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> = _self.subList(fromIndex: fromIndex, toIndex: toIndex)
    return _result.map { it in it as! NSObject? ?? NSNull() }
}

@_cdecl("kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableCollection_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableCollection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableCollection
    let _result: Swift.Bool = _self.addAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableCollection_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableCollection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableCollection
    let _result: Swift.Bool = _self.add(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_MutableCollection_clear__reverse_swift")
package func kotlin_collections_MutableCollection_clear__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableCollection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableCollection
    let _result: Swift.Void = _self.clear()
    return { _result; return true }()
}

@_cdecl("kotlin_collections_MutableCollection_iterator__reverse_swift")
package func kotlin_collections_MutableCollection_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableCollection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableCollection
    let _result: any ExportedKotlinPackages.kotlin.collections.MutableIterator = _self.iterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableCollection_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableCollection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableCollection
    let _result: Swift.Bool = _self.removeAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableCollection_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableCollection_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableCollection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableCollection
    let _result: Swift.Bool = _self.remove(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableCollection_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableCollection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableCollection
    let _result: Swift.Bool = _self.retainAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableIterable_iterator__reverse_swift")
package func kotlin_collections_MutableIterable_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableIterable.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableIterable
    let _result: any ExportedKotlinPackages.kotlin.collections.MutableIterator = _self.iterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_MutableIterator_remove__reverse_swift")
package func kotlin_collections_MutableIterator_remove__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableIterator
    let _result: Swift.Void = _self.remove()
    return { _result; return true }()
}

@_cdecl("kotlin_collections_MutableListIterator_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableListIterator_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableListIterator
    let _result: Swift.Void = _self.add(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return { _result; return true }()
}

@_cdecl("kotlin_collections_MutableListIterator_hasNext__reverse_swift")
package func kotlin_collections_MutableListIterator_hasNext__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableListIterator
    let _result: Swift.Bool = _self.hasNext()
    return _result
}

@_cdecl("kotlin_collections_MutableListIterator_next__reverse_swift")
package func kotlin_collections_MutableListIterator_next__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableListIterator
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.next()
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_collections_MutableListIterator_remove__reverse_swift")
package func kotlin_collections_MutableListIterator_remove__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableListIterator
    let _result: Swift.Void = _self.remove()
    return { _result; return true }()
}

@_cdecl("kotlin_collections_MutableListIterator_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableListIterator_set__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableListIterator
    let _result: Swift.Void = _self.set(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return { _result; return true }()
}

@_cdecl("kotlin_collections_MutableList_addAll__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableList_addAll__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Bool = _self.addAll(index: index, elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableList_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableList_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Bool = _self.addAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableList_add__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableList_add__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Void = _self.add(index: index, element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return { _result; return true }()
}

@_cdecl("kotlin_collections_MutableList_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableList_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Bool = _self.add(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_MutableList_clear__reverse_swift")
package func kotlin_collections_MutableList_clear__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Void = _self.clear()
    return { _result; return true }()
}

@_cdecl("kotlin_collections_MutableList_listIterator__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlin_collections_MutableList_listIterator__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: any ExportedKotlinPackages.kotlin.collections.MutableListIterator = _self.listIterator(index: index)
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_MutableList_listIterator__reverse_swift")
package func kotlin_collections_MutableList_listIterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: any ExportedKotlinPackages.kotlin.collections.MutableListIterator = _self.listIterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_MutableList_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableList_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Bool = _self.removeAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableList_removeAt__TypesOfArguments__Swift_Int32____reverse_swift")
package func kotlin_collections_MutableList_removeAt__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self.removeAt(index: index)
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_collections_MutableList_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableList_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Bool = _self.remove(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_MutableList_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableList_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Bool = _self.retainAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableList_set__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableList_set__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.UnsafeMutableRawPointer? {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self._set(index: index, element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("kotlin_collections_MutableList_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift")
package func kotlin_collections_MutableList_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ fromIndex: Swift.Int32, _ toIndex: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList
    let _result: any ExportedKotlinPackages.kotlin.collections.MutableList = _self.subList(fromIndex: fromIndex, toIndex: toIndex)
    return _result.__externalRCRef()
}
