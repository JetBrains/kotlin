@_implementationOnly import KotlinBridges_ListExport
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

public func foo() -> any KotlinRuntimeSupport.TypedMutableList<Swift.String> {
    return KotlinRuntimeSupport.TypedMutableListImpl<Swift.String>(rawCollection: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___foo(), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList, conformsTo: Swift.String.Type.self)
}
public func testListAny(
    l: any KotlinRuntimeSupport.TypedList<any KotlinRuntimeSupport._KotlinBridgeable>
) -> any KotlinRuntimeSupport.TypedList<any KotlinRuntimeSupport._KotlinBridgeable> {
    return KotlinRuntimeSupport.TypedListImpl<any KotlinRuntimeSupport._KotlinBridgeable>(rawCollection: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListAny__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_anyU20KotlinRuntimeSupport__KotlinBridgeable___(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: KotlinRuntimeSupport._KotlinBridgeable.Type.self)
}
public func testListInt(
    l: any KotlinRuntimeSupport.TypedList<Swift.Int32>
) -> any KotlinRuntimeSupport.TypedList<Swift.Int32> {
    return KotlinRuntimeSupport.TypedListImpl<Swift.Int32>(rawCollection: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListInt__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Int32___(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: Swift.Int32.Type.self)
}
public func testListListInt(
    l: any KotlinRuntimeSupport.TypedList<any KotlinRuntimeSupport.TypedList<Swift.Int32>>
) -> any KotlinRuntimeSupport.TypedList<any KotlinRuntimeSupport.TypedList<Swift.Int32>> {
    return KotlinRuntimeSupport.TypedListImpl<any KotlinRuntimeSupport.TypedList<Swift.Int32>>(rawCollection: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListListInt__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_anyU20KotlinRuntimeSupport_TypedList_Swift_Int32____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: KotlinRuntimeSupport.TypedList<Swift.Int32>.Type.self)
}
public func testListNothing(
    l: any KotlinRuntimeSupport.TypedList<Swift.Never>
) -> any KotlinRuntimeSupport.TypedList<Swift.Never> {
    return KotlinRuntimeSupport.TypedListImpl<Swift.Never>(rawCollection: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListNothing__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Never___(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: Swift.Never.Type.self)
}
public func testListOptAny(
    l: any KotlinRuntimeSupport.TypedList<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
) -> any KotlinRuntimeSupport.TypedList<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
    return KotlinRuntimeSupport.TypedListImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(rawCollection: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListOptAny__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: KotlinRuntimeSupport._KotlinBridgeable.Type.self)
}
public func testListOptInt(
    l: any KotlinRuntimeSupport.TypedList<Swift.Optional<Swift.Int32>>
) -> any KotlinRuntimeSupport.TypedList<Swift.Optional<Swift.Int32>> {
    return KotlinRuntimeSupport.TypedListImpl<Swift.Optional<Swift.Int32>>(rawCollection: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListOptInt__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Optional_Swift_Int32____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: Swift.Int32.Type.self)
}
public func testListOptListInt(
    l: any KotlinRuntimeSupport.TypedList<Swift.Optional<any KotlinRuntimeSupport.TypedList<Swift.Int32>>>
) -> any KotlinRuntimeSupport.TypedList<Swift.Optional<any KotlinRuntimeSupport.TypedList<Swift.Int32>>> {
    return KotlinRuntimeSupport.TypedListImpl<Swift.Optional<any KotlinRuntimeSupport.TypedList<Swift.Int32>>>(rawCollection: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListOptListInt__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Optional_anyU20KotlinRuntimeSupport_TypedList_Swift_Int32_____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: KotlinRuntimeSupport.TypedList<Swift.Int32>.Type.self)
}
public func testListOptNothing(
    l: any KotlinRuntimeSupport.TypedList<Swift.Optional<Swift.Never>>
) -> any KotlinRuntimeSupport.TypedList<Swift.Optional<Swift.Never>> {
    return KotlinRuntimeSupport.TypedListImpl<Swift.Optional<Swift.Never>>(rawCollection: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListOptNothing__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Optional_Swift_Never____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: Swift.Never.Type.self)
}
public func testListOptString(
    l: any KotlinRuntimeSupport.TypedList<Swift.Optional<Swift.String>>
) -> any KotlinRuntimeSupport.TypedList<Swift.Optional<Swift.String>> {
    return KotlinRuntimeSupport.TypedListImpl<Swift.Optional<Swift.String>>(rawCollection: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListOptString__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Optional_Swift_String____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: Swift.String.Type.self)
}
public func testListShort(
    l: any KotlinRuntimeSupport.TypedList<Swift.Int16>
) -> any KotlinRuntimeSupport.TypedList<Swift.Int16> {
    return KotlinRuntimeSupport.TypedListImpl<Swift.Int16>(rawCollection: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListShort__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_Int16___(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: Swift.Int16.Type.self)
}
public func testListString(
    l: any KotlinRuntimeSupport.TypedList<Swift.String>
) -> any KotlinRuntimeSupport.TypedList<Swift.String> {
    return KotlinRuntimeSupport.TypedListImpl<Swift.String>(rawCollection: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListString__TypesOfArguments__anyU20KotlinRuntimeSupport_TypedList_Swift_String___(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: Swift.String.Type.self)
}
public func testOptListInt(
    l: (any KotlinRuntimeSupport.TypedList<Swift.Int32>)?
) -> (any KotlinRuntimeSupport.TypedList<Swift.Int32>)? {
    return { switch __root___testOptListInt__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport_TypedList_Swift_Int32____(l.map { it in it.__rawCollection.__externalRCRef() } ?? nil) { case nil: .none; case let res?: KotlinRuntimeSupport.TypedListImpl<Swift.Int32>(rawCollection: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: Swift.Int32.Type.self); } }()
}
public func testStarList(
    l: any ExportedKotlinPackages.kotlin.collections.List
) -> any ExportedKotlinPackages.kotlin.collections.List {
    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testStarList__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_List__(l.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
}
