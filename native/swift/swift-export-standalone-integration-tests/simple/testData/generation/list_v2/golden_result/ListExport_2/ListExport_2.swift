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
    public protocol MyList_Typed<Element>: KotlinRuntimeSupport.TypedList {
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
    public static func testListOptAny(
        l: any ExportedKotlinPackages.list2.MyList_Typed<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
    ) -> any ExportedKotlinPackages.list2.MyList_Typed<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
        return ExportedKotlinPackages.list2.MyList_TypedImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: list2_testListOptAny__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList_Typed_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.list2.MyList.Type.self) as! any ExportedKotlinPackages.list2.MyList, conformsTo: { wrapperClass in wrapperClass is any KotlinRuntimeSupport._KotlinBridgeable })
    }
    public static func testStarList(
        l: any ExportedKotlinPackages.list2.MyList
    ) -> any ExportedKotlinPackages.list2.MyList {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: list2_testStarList__TypesOfArguments__anyU20ExportedKotlinPackages_list2_MyList__(l.__externalRCRef()), conformsTo: ExportedKotlinPackages.list2.MyList.Type.self) as! any ExportedKotlinPackages.list2.MyList
    }
}
