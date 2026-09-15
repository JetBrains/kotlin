@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_ListExport_2
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

@_documentation(visibility: internal)
extension ExportedKotlinPackages.list2.MyList where Self : ExportedKotlinPackages.list2.__MyList {
}
extension ExportedKotlinPackages.list2.MyList {
}
extension ExportedKotlinPackages.list2.MyList_Typed {
    public var rawList: ExportedKotlinPackages.list2.MyList {
        get {
            return __rawCollection as! ExportedKotlinPackages.list2.MyList
        }
    }
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistential: ExportedKotlinPackages.list2.MyList, ExportedKotlinPackages.list2.__MyList where Wrapped : ExportedKotlinPackages.list2._MyList {
}
@_documentation(visibility: internal)
extension KotlinRuntimeSupport._KotlinExistentialPenBox: ExportedKotlinPackages.list2._MyList {
}
extension ExportedKotlinPackages.list2 {
    public protocol MyList: KotlinRuntime.KotlinBase, ExportedKotlinPackages.kotlin.collections.List, ExportedKotlinPackages.list2._MyList {
    }
    public protocol MyList_Typed<Element>: ExportedKotlinPackages.kotlin.collections.List_Typed {
    }
    @objc(_ExportedKotlinPackages_list2_MyList)
    public protocol _MyList: ExportedKotlinPackages.kotlin.collections._List {
    }
    public protocol __MyList: KotlinRuntimeSupport._KotlinBridgeable, ExportedKotlinPackages.kotlin.collections.__List {
    }
    public struct MyList_TypedImpl<Element>: ExportedKotlinPackages.list2.MyList_Typed {
        public let __conformsTo: (Swift.AnyClass?) -> Swift.Bool
        public let __rawCollection: KotlinRuntime.KotlinBase
        package init(
            rawList: ExportedKotlinPackages.list2.MyList,
            conformsTo: @escaping (Swift.AnyClass?) -> Swift.Bool
        ) {
            self.__rawCollection = rawList
            self.__conformsTo = conformsTo
        }
    }
    public static func testListAny(
        l: any ExportedKotlinPackages.list2.MyList_Typed<any KotlinRuntimeSupport._KotlinBridgeable>
    ) -> any ExportedKotlinPackages.list2.MyList_Typed<any KotlinRuntimeSupport._KotlinBridgeable> {
        return ExportedKotlinPackages.list2.MyList_TypedImpl<any KotlinRuntimeSupport._KotlinBridgeable>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: list2_testListAny__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_anyU20KotlinRuntimeSupport__KotlinBridgeable___(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.list2.MyList.Type.self) as! any ExportedKotlinPackages.list2.MyList, conformsTo: { $0 is any KotlinRuntimeSupport._KotlinBridgeable })
    }
    public static func testListInt(
        l: any ExportedKotlinPackages.list2.MyList_Typed<Swift.Int32>
    ) -> any ExportedKotlinPackages.list2.MyList_Typed<Swift.Int32> {
        return ExportedKotlinPackages.list2.MyList_TypedImpl<Swift.Int32>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: list2_testListInt__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Int32___(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.list2.MyList.Type.self) as! any ExportedKotlinPackages.list2.MyList, conformsTo: { $0 is Swift.Int32 })
    }
    public static func testListListInt(
        l: any ExportedKotlinPackages.list2.MyList_Typed<any ExportedKotlinPackages.list2.MyList_Typed<Swift.Int32>>
    ) -> any ExportedKotlinPackages.list2.MyList_Typed<any ExportedKotlinPackages.list2.MyList_Typed<Swift.Int32>> {
        return ExportedKotlinPackages.list2.MyList_TypedImpl<any ExportedKotlinPackages.list2.MyList_Typed<Swift.Int32>>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: list2_testListListInt__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Int32____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.list2.MyList.Type.self) as! any ExportedKotlinPackages.list2.MyList, conformsTo: { $0 is any ExportedKotlinPackages.list2.MyList_Typed<Swift.Int32> })
    }
    public static func testListNothing(
        l: any ExportedKotlinPackages.list2.MyList_Typed<Swift.Never>
    ) -> any ExportedKotlinPackages.list2.MyList_Typed<Swift.Never> {
        return ExportedKotlinPackages.list2.MyList_TypedImpl<Swift.Never>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: list2_testListNothing__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Never___(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.list2.MyList.Type.self) as! any ExportedKotlinPackages.list2.MyList, conformsTo: { $0 is Swift.Never })
    }
    public static func testListOptAny(
        l: any ExportedKotlinPackages.list2.MyList_Typed<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any ExportedKotlinPackages.list2.MyList_Typed<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return ExportedKotlinPackages.list2.MyList_TypedImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: list2_testListOptAny__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.list2.MyList.Type.self) as! any ExportedKotlinPackages.list2.MyList, conformsTo: { $0 is any KotlinRuntimeSupport._KotlinBridgeable })
    }
    public static func testListOptInt(
        l: any ExportedKotlinPackages.list2.MyList_Typed<Swift.Optional<Swift.Int32>>
    ) -> any ExportedKotlinPackages.list2.MyList_Typed<Swift.Optional<Swift.Int32>> {
        return ExportedKotlinPackages.list2.MyList_TypedImpl<Swift.Optional<Swift.Int32>>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: list2_testListOptInt__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Optional_Swift_Int32____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.list2.MyList.Type.self) as! any ExportedKotlinPackages.list2.MyList, conformsTo: { $0 is Swift.Int32 })
    }
    public static func testListOptListInt(
        l: any ExportedKotlinPackages.list2.MyList_Typed<Swift.Optional<any ExportedKotlinPackages.list2.MyList_Typed<Swift.Int32>>>
    ) -> any ExportedKotlinPackages.list2.MyList_Typed<Swift.Optional<any ExportedKotlinPackages.list2.MyList_Typed<Swift.Int32>>> {
        return ExportedKotlinPackages.list2.MyList_TypedImpl<Swift.Optional<any ExportedKotlinPackages.list2.MyList_Typed<Swift.Int32>>>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: list2_testListOptListInt__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Optional_anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Int32_____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.list2.MyList.Type.self) as! any ExportedKotlinPackages.list2.MyList, conformsTo: { $0 is any ExportedKotlinPackages.list2.MyList_Typed<Swift.Int32> })
    }
    public static func testListOptNothing(
        l: any ExportedKotlinPackages.list2.MyList_Typed<Swift.Optional<Swift.Never>>
    ) -> any ExportedKotlinPackages.list2.MyList_Typed<Swift.Optional<Swift.Never>> {
        return ExportedKotlinPackages.list2.MyList_TypedImpl<Swift.Optional<Swift.Never>>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: list2_testListOptNothing__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Optional_Swift_Never____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.list2.MyList.Type.self) as! any ExportedKotlinPackages.list2.MyList, conformsTo: { $0 is Swift.Never })
    }
    public static func testListOptString(
        l: any ExportedKotlinPackages.list2.MyList_Typed<Swift.Optional<Swift.String>>
    ) -> any ExportedKotlinPackages.list2.MyList_Typed<Swift.Optional<Swift.String>> {
        return ExportedKotlinPackages.list2.MyList_TypedImpl<Swift.Optional<Swift.String>>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: list2_testListOptString__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Optional_Swift_String____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.list2.MyList.Type.self) as! any ExportedKotlinPackages.list2.MyList, conformsTo: { $0 is Swift.String })
    }
    public static func testListShort(
        l: any ExportedKotlinPackages.list2.MyList_Typed<Swift.Int16>
    ) -> any ExportedKotlinPackages.list2.MyList_Typed<Swift.Int16> {
        return ExportedKotlinPackages.list2.MyList_TypedImpl<Swift.Int16>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: list2_testListShort__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Int16___(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.list2.MyList.Type.self) as! any ExportedKotlinPackages.list2.MyList, conformsTo: { $0 is Swift.Int16 })
    }
    public static func testListString(
        l: any ExportedKotlinPackages.list2.MyList_Typed<Swift.String>
    ) -> any ExportedKotlinPackages.list2.MyList_Typed<Swift.String> {
        return ExportedKotlinPackages.list2.MyList_TypedImpl<Swift.String>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: list2_testListString__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_String___(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.list2.MyList.Type.self) as! any ExportedKotlinPackages.list2.MyList, conformsTo: { $0 is Swift.String })
    }
    public static func testOptListInt(
        l: (any ExportedKotlinPackages.list2.MyList_Typed<Swift.Int32>)?
    ) -> (any ExportedKotlinPackages.list2.MyList_Typed<Swift.Int32>)? {
        return { switch list2_testOptListInt__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Int32____(l.map { it in it.__rawCollection.__externalRCRef() } ?? nil) { case nil: .none; case let res?: ExportedKotlinPackages.list2.MyList_TypedImpl<Swift.Int32>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: ExportedKotlinPackages.list2.MyList.Type.self) as! any ExportedKotlinPackages.list2.MyList, conformsTo: { $0 is Swift.Int32 }); } }()
    }
    public static func testStarList(
        l: any ExportedKotlinPackages.list2.MyList
    ) -> any ExportedKotlinPackages.list2.MyList {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: list2_testStarList__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList__(l.__externalRCRef()), conformsTo: ExportedKotlinPackages.list2.MyList.Type.self) as! any ExportedKotlinPackages.list2.MyList
    }
}
