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
extension ExportedKotlinPackages.kotlin.collections.MutableSet where Self : ExportedKotlinPackages.kotlin.collections.__MutableSet {
    public func add(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func addAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func clear() -> Swift.Void {
        return { kotlin_collections_MutableSet_clear(self.__externalRCRef()); return () }()
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_MutableSet_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableIterator
    }
    public func remove(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func removeAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func retainAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
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
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return kotlin_collections_Set_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_Set_isEmpty(self.__externalRCRef())
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_Set_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.Iterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public static func ~=(
        this: Self,
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this.contains(element: element)
    }
}
extension ExportedKotlinPackages.kotlin.collections.Set {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableSet, ExportedKotlinPackages.kotlin.collections.__MutableSet where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableSet {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections.__Collection where Wrapped : ExportedKotlinPackages.kotlin.collections._Collection {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableCollection, ExportedKotlinPackages.kotlin.collections.__MutableCollection where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableCollection {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableIterator, ExportedKotlinPackages.kotlin.collections.__MutableIterator where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableIterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Set, ExportedKotlinPackages.kotlin.collections.__Set where Wrapped : ExportedKotlinPackages.kotlin.collections._Set {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Iterable, ExportedKotlinPackages.kotlin.collections.__Iterable where Wrapped : ExportedKotlinPackages.kotlin.collections._Iterable {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.MutableIterable, ExportedKotlinPackages.kotlin.collections.__MutableIterable where Wrapped : ExportedKotlinPackages.kotlin.collections._MutableIterable {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Iterator, ExportedKotlinPackages.kotlin.collections.__Iterator where Wrapped : ExportedKotlinPackages.kotlin.collections._Iterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableSet {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Collection {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableCollection {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableIterator {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Set {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Iterable {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._MutableIterable {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.kotlin.collections._Iterator {
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
    public protocol MutableSet: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Set, ExportedKotlinPackages.kotlin.collections.MutableCollection, ExportedKotlinPackages.kotlin.collections._MutableSet {
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
    public protocol Set: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection, ExportedKotlinPackages.kotlin.collections._Set {
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

@_cdecl("kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableSet_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableSet.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableSet
    let _result: Swift.Bool = _self.addAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableSet_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableSet_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableSet.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableSet
    let _result: Swift.Bool = _self.add(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_MutableSet_clear__reverse_swift")
package func kotlin_collections_MutableSet_clear__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableSet.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableSet
    let _result: Swift.Void = _self.clear()
    return { _result; return true }()
}

@_cdecl("kotlin_collections_MutableSet_iterator__reverse_swift")
package func kotlin_collections_MutableSet_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableSet.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableSet
    let _result: any ExportedKotlinPackages.kotlin.collections.MutableIterator = _self.iterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableSet_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableSet.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableSet
    let _result: Swift.Bool = _self.removeAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_MutableSet_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_MutableSet_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableSet.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableSet
    let _result: Swift.Bool = _self.remove(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_MutableSet_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.MutableSet.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableSet
    let _result: Swift.Bool = _self.retainAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func kotlin_collections_Set_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Set.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Set
    let _result: Swift.Bool = _self.containsAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("kotlin_collections_Set_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func kotlin_collections_Set_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Set.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Set
    let _result: Swift.Bool = _self.contains(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("kotlin_collections_Set_isEmpty__reverse_swift")
package func kotlin_collections_Set_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Set.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Set
    let _result: Swift.Bool = _self.isEmpty()
    return _result
}

@_cdecl("kotlin_collections_Set_iterator__reverse_swift")
package func kotlin_collections_Set_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Set.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Set
    let _result: any ExportedKotlinPackages.kotlin.collections.Iterator = _self.iterator()
    return _result.__externalRCRef()
}

@_cdecl("kotlin_collections_Set_size_get__reverse_swift")
package func kotlin_collections_Set_size_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: `self`, conformsTo: ExportedKotlinPackages.kotlin.collections.Set.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Set
    let _result: Swift.Int32 = _self.size
    return _result
}
