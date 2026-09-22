@_implementationOnly import KotlinBridges_CollectionsV2
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

public protocol MyListImpl_Typed<Element>: KotlinRuntimeSupport.TypedList {
}
public protocol MyMutableListImpl_Typed<Element>: KotlinRuntimeSupport.TypedMutableList, CollectionsV2.MyListImpl_Typed {
}
open class MyListImpl: KotlinRuntime.KotlinBase {
    open var size: Swift.Int32 {
        get {
            if Self.self == CollectionsV2.MyListImpl.self {
                return MyListImpl_size_get(self.__externalRCRef())
            } else {
                return MyListImpl_size_get_direct(self.__externalRCRef())
            }
        }
    }
    public init(
        impl: any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) {
         let __kt: Swift.UnsafeMutableRawPointer!
         if Self.self == CollectionsV2.MyListImpl.self {
             __kt = __root___MyListImpl_init_allocate()
         } else {
             __kt = _kotlinAllocInstanceForSwiftSubclass(Self.self)
         }
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___MyListImpl_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_collections_List_Typed_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(__kt, impl.__rawCollection.__externalRCRef()); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    open func _get(
        index: Swift.Int32
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        if Self.self == CollectionsV2.MyListImpl.self {
            return { switch MyListImpl_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        } else {
            return { switch MyListImpl_get__TypesOfArguments__Swift_Int32___direct(self.__externalRCRef(), index) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
        }
    }
    open func contains(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        if Self.self == CollectionsV2.MyListImpl.self {
            return MyListImpl_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
        } else {
            return MyListImpl_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____direct(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
        }
    }
    open func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        if Self.self == CollectionsV2.MyListImpl.self {
            return MyListImpl_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
        } else {
            return MyListImpl_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection___direct(self.__externalRCRef(), elements.__externalRCRef())
        }
    }
    open func indexOf(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Int32 {
        if Self.self == CollectionsV2.MyListImpl.self {
            return MyListImpl_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
        } else {
            return MyListImpl_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____direct(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
        }
    }
    open func isEmpty() -> Swift.Bool {
        if Self.self == CollectionsV2.MyListImpl.self {
            return MyListImpl_isEmpty(self.__externalRCRef())
        } else {
            return MyListImpl_isEmpty_direct(self.__externalRCRef())
        }
    }
    open func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        if Self.self == CollectionsV2.MyListImpl.self {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: MyListImpl_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.Iterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterator
        } else {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: MyListImpl_iterator_direct(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.Iterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Iterator
        }
    }
    open func lastIndexOf(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Int32 {
        if Self.self == CollectionsV2.MyListImpl.self {
            return MyListImpl_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
        } else {
            return MyListImpl_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____direct(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
        }
    }
    open func listIterator() -> any ExportedKotlinPackages.kotlin.collections.ListIterator {
        if Self.self == CollectionsV2.MyListImpl.self {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: MyListImpl_listIterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
        } else {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: MyListImpl_listIterator_direct(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
        }
    }
    open func listIterator(
        index: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.collections.ListIterator {
        if Self.self == CollectionsV2.MyListImpl.self {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: MyListImpl_listIterator__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index), conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
        } else {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: MyListImpl_listIterator__TypesOfArguments__Swift_Int32___direct(self.__externalRCRef(), index), conformsTo: ExportedKotlinPackages.kotlin.collections.ListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.ListIterator
        }
    }
    open func subList(
        fromIndex: Swift.Int32,
        toIndex: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        if Self.self == CollectionsV2.MyListImpl.self {
            return ExportedKotlinPackages.kotlin.collections.List_TypedImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: MyListImpl_subList__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), fromIndex, toIndex), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: { $0 is any KotlinRuntimeSupport._KotlinBridgeable })
        } else {
            return ExportedKotlinPackages.kotlin.collections.List_TypedImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: MyListImpl_subList__TypesOfArguments__Swift_Int32_Swift_Int32___direct(self.__externalRCRef(), fromIndex, toIndex), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: { $0 is any KotlinRuntimeSupport._KotlinBridgeable })
        }
    }
    public static func ~=(
        this: CollectionsV2.MyListImpl,
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this.contains(element: element)
    }
    open subscript(
        index: Swift.Int32
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get {
            _get(index: index)
        }
    }
}
public final class MyMutableListImpl: CollectionsV2.MyListImpl {
    public override var size: Swift.Int32 {
        get {
            return MyMutableListImpl_size_get(self.__externalRCRef())
        }
    }
    public init(
        impl: any ExportedKotlinPackages.kotlin.collections.MutableList_Typed<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) {
        let __kt = __root___MyMutableListImpl_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
        { __root___MyMutableListImpl_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_anyU20ExportedKotlinPackages_kotlin_collections_MutableList_Typed_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(__kt, impl.__rawCollection.__externalRCRef()); return () }()
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
    }
    public override func _get(
        index: Swift.Int32
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch MyMutableListImpl_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func _set(
        index: Swift.Int32,
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch MyMutableListImpl_set__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), index, element.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func add(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return MyMutableListImpl_add__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func add(
        index: Swift.Int32,
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Void {
        return { MyMutableListImpl_add__TypesOfArguments__Swift_Int32_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), index, element.map { it in it.__externalRCRef() } ?? nil); return () }()
    }
    public func addAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return MyMutableListImpl_addAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func addAll(
        index: Swift.Int32,
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return MyMutableListImpl_addAll__TypesOfArguments__Swift_Int32_anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), index, elements.__externalRCRef())
    }
    public func clear() -> Swift.Void {
        return { MyMutableListImpl_clear(self.__externalRCRef()); return () }()
    }
    public override func contains(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return MyMutableListImpl_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public override func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return MyMutableListImpl_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public override func indexOf(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Int32 {
        return MyMutableListImpl_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public override func isEmpty() -> Swift.Bool {
        return MyMutableListImpl_isEmpty(self.__externalRCRef())
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.MutableIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: MyMutableListImpl_iterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableIterator
    }
    public override func lastIndexOf(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Int32 {
        return MyMutableListImpl_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func listIterator() -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: MyMutableListImpl_listIterator(self.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableListIterator
    }
    public func listIterator(
        index: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.collections.MutableListIterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: MyMutableListImpl_listIterator__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableListIterator.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableListIterator
    }
    public func remove(
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        return MyMutableListImpl_remove__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func removeAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return MyMutableListImpl_removeAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func removeAt(
        index: Swift.Int32
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch MyMutableListImpl_removeAt__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
    }
    public func retainAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return MyMutableListImpl_retainAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func subList(
        fromIndex: Swift.Int32,
        toIndex: Swift.Int32
    ) -> any ExportedKotlinPackages.kotlin.collections.MutableList_Typed<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return ExportedKotlinPackages.kotlin.collections.MutableList_TypedImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: MyMutableListImpl_subList__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), fromIndex, toIndex), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList, conformsTo: { $0 is any KotlinRuntimeSupport._KotlinBridgeable })
    }
    public static func ~=(
        this: CollectionsV2.MyMutableListImpl,
        element: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> Swift.Bool {
        this.contains(element: element)
    }
    public override subscript(
        index: Swift.Int32
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        get {
            _get(index: index)
        }
        set(element) {
            _set(index: index, element: element)
        }
    }
}
public struct MyListImpl_TypedImpl<Element>: CollectionsV2.MyListImpl_Typed {
    public let __conformsTo: (Swift.AnyClass?) -> Swift.Bool
    public let __rawCollection: KotlinRuntime.KotlinBase
    package init(
        rawList: CollectionsV2.MyListImpl,
        conformsTo: @escaping (Swift.AnyClass?) -> Swift.Bool
    ) {
        self.__rawCollection = rawList
        self.__conformsTo = conformsTo
    }
}
public struct MyMutableListImpl_TypedImpl<Element>: CollectionsV2.MyMutableListImpl_Typed {
    public let __conformsTo: (Swift.AnyClass?) -> Swift.Bool
    public let __rawCollection: KotlinRuntime.KotlinBase
    package init(
        rawList: CollectionsV2.MyMutableListImpl,
        conformsTo: @escaping (Swift.AnyClass?) -> Swift.Bool
    ) {
        self.__rawCollection = rawList
        self.__conformsTo = conformsTo
    }
}
public func testMyListImplInt(
    l: any CollectionsV2.MyListImpl_Typed<Swift.Int32>
) -> any CollectionsV2.MyListImpl_Typed<Swift.Int32> {
    return CollectionsV2.MyListImpl_TypedImpl<Swift.Int32>(rawList: CollectionsV2.MyListImpl.__createClassWrapper(externalRCRef: __root___testMyListImplInt__TypesOfArguments__anyU20CollectionsV2_MyListImpl_Typed_Swift_Int32___(l.__rawCollection.__externalRCRef())), conformsTo: { $0 is Swift.Int32 })
}
public func testMyMutableListImplInt(
    l: any CollectionsV2.MyMutableListImpl_Typed<Swift.Int32>
) -> any CollectionsV2.MyMutableListImpl_Typed<Swift.Int32> {
    return CollectionsV2.MyMutableListImpl_TypedImpl<Swift.Int32>(rawList: CollectionsV2.MyMutableListImpl.__createClassWrapper(externalRCRef: __root___testMyMutableListImplInt__TypesOfArguments__anyU20CollectionsV2_MyMutableListImpl_Typed_Swift_Int32___(l.__rawCollection.__externalRCRef())), conformsTo: { $0 is Swift.Int32 })
}
extension CollectionsV2.MyListImpl_Typed {
    public var rawList: CollectionsV2.MyListImpl {
        get {
            return __rawCollection as! CollectionsV2.MyListImpl
        }
    }
}
extension CollectionsV2.MyMutableListImpl_Typed {
    public var rawList: CollectionsV2.MyMutableListImpl {
        get {
            return __rawCollection as! CollectionsV2.MyMutableListImpl
        }
    }
}
@_cdecl("MyListImpl_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift")
package func MyListImpl_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ elements: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = CollectionsV2.MyListImpl.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Bool = _self.containsAll(elements: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: elements, conformsTo: ExportedKotlinPackages.kotlin.collections.Collection.Type.self) as! any ExportedKotlinPackages.kotlin.collections.Collection)
    return _result
}

@_cdecl("MyListImpl_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func MyListImpl_contains__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Bool {
    let _self = CollectionsV2.MyListImpl.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Bool = _self.contains(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("MyListImpl_get__TypesOfArguments__Swift_Int32____reverse_swift")
package func MyListImpl_get__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer? {
    let _self = CollectionsV2.MyListImpl.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable> = _self._get(index: index)
    return _result.map { it in it.__externalRCRef() } ?? nil
}

@_cdecl("MyListImpl_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func MyListImpl_indexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _self = CollectionsV2.MyListImpl.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int32 = _self.indexOf(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("MyListImpl_isEmpty__reverse_swift")
package func MyListImpl_isEmpty__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Bool {
    let _self = CollectionsV2.MyListImpl.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Bool = _self.isEmpty()
    return _result
}

@_cdecl("MyListImpl_iterator__reverse_swift")
package func MyListImpl_iterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = CollectionsV2.MyListImpl.__createClassWrapper(externalRCRef: `self`)!
    let _result: any ExportedKotlinPackages.kotlin.collections.Iterator = _self.iterator()
    return _result.__externalRCRef()
}

@_cdecl("MyListImpl_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift")
package func MyListImpl_lastIndexOf__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable_____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ element: Swift.UnsafeMutableRawPointer?) -> Swift.Int32 {
    let _self = CollectionsV2.MyListImpl.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int32 = _self.lastIndexOf(element: { switch element { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }())
    return _result
}

@_cdecl("MyListImpl_listIterator__TypesOfArguments__Swift_Int32____reverse_swift")
package func MyListImpl_listIterator__TypesOfArguments__Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ index: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = CollectionsV2.MyListImpl.__createClassWrapper(externalRCRef: `self`)!
    let _result: any ExportedKotlinPackages.kotlin.collections.ListIterator = _self.listIterator(index: index)
    return _result.__externalRCRef()
}

@_cdecl("MyListImpl_listIterator__reverse_swift")
package func MyListImpl_listIterator__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.UnsafeMutableRawPointer {
    let _self = CollectionsV2.MyListImpl.__createClassWrapper(externalRCRef: `self`)!
    let _result: any ExportedKotlinPackages.kotlin.collections.ListIterator = _self.listIterator()
    return _result.__externalRCRef()
}

@_cdecl("MyListImpl_size_get__reverse_swift")
package func MyListImpl_size_get__reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer) -> Swift.Int32 {
    let _self = CollectionsV2.MyListImpl.__createClassWrapper(externalRCRef: `self`)!
    let _result: Swift.Int32 = _self.size
    return _result
}

@_cdecl("MyListImpl_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift")
package func MyListImpl_subList__TypesOfArguments__Swift_Int32_Swift_Int32____reverse_swift(_ `self`: Swift.UnsafeMutableRawPointer, _ fromIndex: Swift.Int32, _ toIndex: Swift.Int32) -> Swift.UnsafeMutableRawPointer {
    let _self = CollectionsV2.MyListImpl.__createClassWrapper(externalRCRef: `self`)!
    let _result: any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> = _self.subList(fromIndex: fromIndex, toIndex: toIndex)
    return _result.__rawCollection.__externalRCRef()
}
