@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_ListExport_2
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

@_documentation(visibility: internal)
extension ExportedKotlinPackages.list2.MyList where Self : ExportedKotlinPackages.list2.__MyList {
}
extension ExportedKotlinPackages.list2.MyList {
    @inlinable
    public var covariant: KotlinRuntimeSupport.KotlinCovariant<T> {
        get {
            return (_owner: self, _variance: KotlinRuntimeSupport._absurd)
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.list2._MyList {
}
extension ExportedKotlinPackages.list2 {
    public protocol MyList<T>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.List, ExportedKotlinPackages.list2._MyList where Self.E == T {
    }
    @objc(_ExportedKotlinPackages_list2_MyList)
    public protocol _MyList: ExportedKotlinPackages.kotlin.collections._List {
    }
    public protocol __MyList: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__List {
    }
    public final class AnyMyList<T: KotlinRuntimeSupport._KotlinBridgeable>: KotlinRuntime.KotlinBase, ExportedKotlinPackages.list2.MyList, ExportedKotlinPackages.list2.__MyList, KotlinRuntimeSupport._KotlinCustomPointerBridgeable {
        public typealias E = T
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
    public static func testListAny(
        l: any ExportedKotlinPackages.list2.MyList<KotlinRuntimeSupport.KotlinAny>
    ) -> any ExportedKotlinPackages.list2.MyList<KotlinRuntimeSupport.KotlinAny> {
        return { let _ptr = list2_testListAny__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_KotlinRuntimeSupport_KotlinAny___(l.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.list2.MyList<KotlinRuntimeSupport.KotlinAny>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.list2.AnyMyList<KotlinRuntimeSupport.KotlinAny>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func testListInt(
        l: any ExportedKotlinPackages.list2.MyList<Swift.Int32>
    ) -> any ExportedKotlinPackages.list2.MyList<Swift.Int32> {
        return { let _ptr = list2_testListInt__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Swift_Int32___(l.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.list2.MyList<Swift.Int32>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.list2.AnyMyList<Swift.Int32>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func testListListInt(
        l: any ExportedKotlinPackages.list2.MyList<ExportedKotlinPackages.list2.AnyMyList<Swift.Int32>>
    ) -> any ExportedKotlinPackages.list2.MyList<ExportedKotlinPackages.list2.AnyMyList<Swift.Int32>> {
        return { let _ptr = list2_testListListInt__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_ExportedKotlinPackages_list2_AnyMyList_Swift_Int32____(l.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.list2.MyList<ExportedKotlinPackages.list2.AnyMyList<Swift.Int32>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.list2.AnyMyList<ExportedKotlinPackages.list2.AnyMyList<Swift.Int32>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func testListNothing(
        l: any ExportedKotlinPackages.list2.MyList<Swift.Never>
    ) -> any ExportedKotlinPackages.list2.MyList<Swift.Never> {
        return { let _ptr = list2_testListNothing__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Swift_Never___(l.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.list2.MyList<Swift.Never>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.list2.AnyMyList<Swift.Never>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func testListOptAny(
        l: any ExportedKotlinPackages.list2.MyList<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>
    ) -> any ExportedKotlinPackages.list2.MyList<Swift.Optional<KotlinRuntimeSupport.KotlinAny>> {
        return { let _ptr = list2_testListOptAny__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Swift_Optional_KotlinRuntimeSupport_KotlinAny____(l.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.list2.MyList<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.list2.AnyMyList<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func testListOptInt(
        l: any ExportedKotlinPackages.list2.MyList<Swift.Optional<Swift.Int32>>
    ) -> any ExportedKotlinPackages.list2.MyList<Swift.Optional<Swift.Int32>> {
        return { let _ptr = list2_testListOptInt__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Swift_Optional_Swift_Int32____(l.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.list2.MyList<Swift.Optional<Swift.Int32>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.list2.AnyMyList<Swift.Optional<Swift.Int32>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func testListOptListInt(
        l: any ExportedKotlinPackages.list2.MyList<Swift.Optional<ExportedKotlinPackages.list2.AnyMyList<Swift.Int32>>>
    ) -> any ExportedKotlinPackages.list2.MyList<Swift.Optional<ExportedKotlinPackages.list2.AnyMyList<Swift.Int32>>> {
        return { let _ptr = list2_testListOptListInt__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Swift_Optional_ExportedKotlinPackages_list2_AnyMyList_Swift_Int32_____(l.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.list2.MyList<Swift.Optional<ExportedKotlinPackages.list2.AnyMyList<Swift.Int32>>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.list2.AnyMyList<Swift.Optional<ExportedKotlinPackages.list2.AnyMyList<Swift.Int32>>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func testListOptNothing(
        l: any ExportedKotlinPackages.list2.MyList<Swift.Optional<Swift.Never>>
    ) -> any ExportedKotlinPackages.list2.MyList<Swift.Optional<Swift.Never>> {
        return { let _ptr = list2_testListOptNothing__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Swift_Optional_Swift_Never____(l.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.list2.MyList<Swift.Optional<Swift.Never>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.list2.AnyMyList<Swift.Optional<Swift.Never>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func testListOptString(
        l: any ExportedKotlinPackages.list2.MyList<Swift.Optional<Swift.String>>
    ) -> any ExportedKotlinPackages.list2.MyList<Swift.Optional<Swift.String>> {
        return { let _ptr = list2_testListOptString__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Swift_Optional_Swift_String____(l.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.list2.MyList<Swift.Optional<Swift.String>>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.list2.AnyMyList<Swift.Optional<Swift.String>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func testListShort(
        l: any ExportedKotlinPackages.list2.MyList<Swift.Int16>
    ) -> any ExportedKotlinPackages.list2.MyList<Swift.Int16> {
        return { let _ptr = list2_testListShort__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Swift_Int16___(l.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.list2.MyList<Swift.Int16>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.list2.AnyMyList<Swift.Int16>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func testListString(
        l: any ExportedKotlinPackages.list2.MyList<Swift.String>
    ) -> any ExportedKotlinPackages.list2.MyList<Swift.String> {
        return { let _ptr = list2_testListString__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Swift_String___(l.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.list2.MyList<Swift.String>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.list2.AnyMyList<Swift.String>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
    public static func testOptListInt(
        l: (any ExportedKotlinPackages.list2.MyList<Swift.Int32>)?
    ) -> (any ExportedKotlinPackages.list2.MyList<Swift.Int32>)? {
        return { () -> (any ExportedKotlinPackages.list2.MyList<Swift.Int32>)? in guard let res = list2_testOptListInt__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_list2_MyList_Swift_Int32____({ if let it = l { return it.__externalRCRef() } else { return nil } }()) else { return nil }; return { let _ptr = res; if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.list2.MyList<Swift.Int32>) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.list2.AnyMyList<Swift.Int32>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }() }()
    }
    public static func testStarList(
        l: any ExportedKotlinPackages.list2.MyList
    ) -> any ExportedKotlinPackages.list2.MyList {
        return { let _ptr = list2_testStarList__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList__(l.__externalRCRef()); if #available(macOS 13.0, iOS 16.0, watchOS 9.0, tvOS 16.0, *), let _w = KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: KotlinRuntimeSupport._cloneBridgeableRef(_ptr)) as? (any ExportedKotlinPackages.list2.MyList) { KotlinRuntimeSupport._disposeBridgeableRef(_ptr); return _w }; return ExportedKotlinPackages.list2.AnyMyList<Swift.Optional<KotlinRuntimeSupport.KotlinAny>>(__externalRCRefUnsafe: _ptr, options: .asExistentialWrapper) }()
    }
}
