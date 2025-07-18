@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_KotlinStdlib
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.kotlin.collections.Collection where Self : KotlinRuntimeSupport._KotlinBridged {
    public var size: Swift.Int32 {
        get {
            return kotlin_collections_Collection_size_get(self.__externalRCRef())
        }
    }
    public func contains(
        element: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        return kotlin_collections_Collection_contains__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
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
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_Collection_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public static func ~=(
        this: Self,
        element: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        this.contains(element: element)
    }
}
extension ExportedKotlinPackages.kotlin.enums.EnumEntries where Self : KotlinRuntimeSupport._KotlinBridged {
}
extension ExportedKotlinPackages.kotlin.collections.Iterable where Self : KotlinRuntimeSupport._KotlinBridged {
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_Iterable_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
}
extension ExportedKotlinPackages.kotlin.collections.Iterator where Self : KotlinRuntimeSupport._KotlinBridged {
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_Iterator_hasNext(self.__externalRCRef())
    }
    public func next() -> KotlinRuntime.KotlinBase? {
        return { switch kotlin_collections_Iterator_next(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
    }
}
extension ExportedKotlinPackages.kotlin.collections.List where Self : KotlinRuntimeSupport._KotlinBridged {
    public var size: Swift.Int32 {
        get {
            return kotlin_collections_List_size_get(self.__externalRCRef())
        }
    }
    public func _get(
        index: Swift.Int32
    ) -> KotlinRuntime.KotlinBase? {
        return { switch kotlin_collections_List_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
    }
    public func contains(
        element: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        return kotlin_collections_List_contains__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func containsAll(
        elements: any ExportedKotlinPackages.kotlin.collections.Collection
    ) -> Swift.Bool {
        return kotlin_collections_List_containsAll__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_Collection__(self.__externalRCRef(), elements.__externalRCRef())
    }
    public func indexOf(
        element: KotlinRuntime.KotlinBase?
    ) -> Swift.Int32 {
        return kotlin_collections_List_indexOf__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
    }
    public func isEmpty() -> Swift.Bool {
        return kotlin_collections_List_isEmpty(self.__externalRCRef())
    }
    public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_collections_List_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public func lastIndexOf(
        element: KotlinRuntime.KotlinBase?
    ) -> Swift.Int32 {
        return kotlin_collections_List_lastIndexOf__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), element.map { it in it.__externalRCRef() } ?? nil)
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
    ) -> [KotlinRuntime.KotlinBase?] {
        return kotlin_collections_List_subList__TypesOfArguments__Swift_Int32_Swift_Int32__(self.__externalRCRef(), fromIndex, toIndex) as! Swift.Array<Swift.Optional<KotlinRuntime.KotlinBase>>
    }
    public static func ~=(
        this: Self,
        element: KotlinRuntime.KotlinBase?
    ) -> Swift.Bool {
        this.contains(element: element)
    }
    public subscript(
        index: Swift.Int32
    ) -> KotlinRuntime.KotlinBase? {
        get {
            _get(index: index)
        }
    }
}
extension ExportedKotlinPackages.kotlin.collections.ListIterator where Self : KotlinRuntimeSupport._KotlinBridged {
    public func hasNext() -> Swift.Bool {
        return kotlin_collections_ListIterator_hasNext(self.__externalRCRef())
    }
    public func hasPrevious() -> Swift.Bool {
        return kotlin_collections_ListIterator_hasPrevious(self.__externalRCRef())
    }
    public func next() -> KotlinRuntime.KotlinBase? {
        return { switch kotlin_collections_ListIterator_next(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
    }
    public func nextIndex() -> Swift.Int32 {
        return kotlin_collections_ListIterator_nextIndex(self.__externalRCRef())
    }
    public func previous() -> KotlinRuntime.KotlinBase? {
        return { switch kotlin_collections_ListIterator_previous(self.__externalRCRef()) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
    }
    public func previousIndex() -> Swift.Int32 {
        return kotlin_collections_ListIterator_previousIndex(self.__externalRCRef())
    }
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.enums.EnumEntries where Wrapped : ExportedKotlinPackages.kotlin.enums._EnumEntries {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Iterator where Wrapped : ExportedKotlinPackages.kotlin.collections._Iterator {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.List where Wrapped : ExportedKotlinPackages.kotlin.collections._List {
}
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.kotlin.collections.Collection where Wrapped : ExportedKotlinPackages.kotlin.collections._Collection {
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
            element: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func isEmpty() -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public protocol Iterable: KotlinRuntime.KotlinBase {
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
    }
    public protocol Iterator: KotlinRuntime.KotlinBase {
        func hasNext() -> Swift.Bool
        func next() -> KotlinRuntime.KotlinBase?
    }
    public protocol List: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Collection {
        var size: Swift.Int32 {
            get
        }
        func _get(
            index: Swift.Int32
        ) -> KotlinRuntime.KotlinBase?
        func contains(
            element: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool
        func containsAll(
            elements: any ExportedKotlinPackages.kotlin.collections.Collection
        ) -> Swift.Bool
        func indexOf(
            element: KotlinRuntime.KotlinBase?
        ) -> Swift.Int32
        func isEmpty() -> Swift.Bool
        func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator
        func lastIndexOf(
            element: KotlinRuntime.KotlinBase?
        ) -> Swift.Int32
        func listIterator() -> any ExportedKotlinPackages.kotlin.collections.ListIterator
        func listIterator(
            index: Swift.Int32
        ) -> any ExportedKotlinPackages.kotlin.collections.ListIterator
        func subList(
            fromIndex: Swift.Int32,
            toIndex: Swift.Int32
        ) -> [KotlinRuntime.KotlinBase?]
    }
    public protocol ListIterator: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.Iterator {
        func hasNext() -> Swift.Bool
        func hasPrevious() -> Swift.Bool
        func next() -> KotlinRuntime.KotlinBase?
        func nextIndex() -> Swift.Int32
        func previous() -> KotlinRuntime.KotlinBase?
        func previousIndex() -> Swift.Int32
    }
    @objc(_Collection)
    protocol _Collection: ExportedKotlinPackages.kotlin.collections._Iterable {
    }
    @objc(_Iterable)
    protocol _Iterable {
    }
    @objc(_Iterator)
    protocol _Iterator {
    }
    @objc(_List)
    protocol _List: ExportedKotlinPackages.kotlin.collections._Collection {
    }
    @objc(_ListIterator)
    protocol _ListIterator: ExportedKotlinPackages.kotlin.collections._Iterator {
    }
}
extension ExportedKotlinPackages.kotlin.enums {
    public protocol EnumEntries: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.List {
    }
    @objc(_EnumEntries)
    protocol _EnumEntries: ExportedKotlinPackages.kotlin.collections._List {
    }
}
extension ExportedKotlinPackages.kotlin {
    public final class Array: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public var size: Swift.Int32 {
            get {
                return kotlin_Array_size_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public init(
            size: Swift.Int32,
            `init`: @escaping (Swift.Int32) -> Swift.Optional<KotlinRuntime.KotlinBase>
        ) {
            fatalError()
        }
        public func _get(
            index: Swift.Int32
        ) -> KotlinRuntime.KotlinBase? {
            return { switch kotlin_Array_get__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), index) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
        }
        public func _set(
            index: Swift.Int32,
            value: KotlinRuntime.KotlinBase?
        ) -> Swift.Void {
            return kotlin_Array_set__TypesOfArguments__Swift_Int32_Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), index, value.map { it in it.__externalRCRef() } ?? nil)
        }
        public func iterator() -> any ExportedKotlinPackages.kotlin.collections.Iterator {
            return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: kotlin_Array_iterator(self.__externalRCRef())) as! any ExportedKotlinPackages.kotlin.collections.Iterator
        }
        public subscript(
            index: Swift.Int32
        ) -> KotlinRuntime.KotlinBase? {
            get {
                _get(index: index)
            }
            set(value) {
                _set(index: index, value: value)
            }
        }
    }
    open class Enum: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public final class Companion: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
            public static var shared: ExportedKotlinPackages.kotlin.Enum.Companion {
                get {
                    return ExportedKotlinPackages.kotlin.Enum.Companion.__createClassWrapper(externalRCRef: kotlin_Enum_Companion_get())
                }
            }
            private init() {
                fatalError()
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
        }
        public final var name: Swift.String {
            get {
                return kotlin_Enum_name_get(self.__externalRCRef())
            }
        }
        public final var ordinal: Swift.Int32 {
            get {
                return kotlin_Enum_ordinal_get(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        package init(
            name: Swift.String,
            ordinal: Swift.Int32
        ) {
            fatalError()
        }
        public static func <(
            this: ExportedKotlinPackages.kotlin.Enum,
            other: ExportedKotlinPackages.kotlin.Enum
        ) -> Swift.Bool {
            this._compareTo(other: other) < 0
        }
        public static func <=(
            this: ExportedKotlinPackages.kotlin.Enum,
            other: ExportedKotlinPackages.kotlin.Enum
        ) -> Swift.Bool {
            this._compareTo(other: other) <= 0
        }
        public static func ==(
            this: ExportedKotlinPackages.kotlin.Enum,
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            this.equals(other: other)
        }
        public static func >(
            this: ExportedKotlinPackages.kotlin.Enum,
            other: ExportedKotlinPackages.kotlin.Enum
        ) -> Swift.Bool {
            this._compareTo(other: other) > 0
        }
        public static func >=(
            this: ExportedKotlinPackages.kotlin.Enum,
            other: ExportedKotlinPackages.kotlin.Enum
        ) -> Swift.Bool {
            this._compareTo(other: other) >= 0
        }
        public final func _compareTo(
            other: ExportedKotlinPackages.kotlin.Enum
        ) -> Swift.Int32 {
            return kotlin_Enum_compareTo__TypesOfArguments__ExportedKotlinPackages_kotlin_Enum__(self.__externalRCRef(), other.__externalRCRef())
        }
        public final func equals(
            other: KotlinRuntime.KotlinBase?
        ) -> Swift.Bool {
            return kotlin_Enum_equals__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(self.__externalRCRef(), other.map { it in it.__externalRCRef() } ?? nil)
        }
        public final func hashCode() -> Swift.Int32 {
            return kotlin_Enum_hashCode(self.__externalRCRef())
        }
        open func toString() -> Swift.String {
            return kotlin_Enum_toString(self.__externalRCRef())
        }
    }
}
