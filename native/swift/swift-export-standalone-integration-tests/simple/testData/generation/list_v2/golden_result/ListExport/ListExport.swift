@_implementationOnly import KotlinBridges_ListExport
import KotlinRuntime
import KotlinRuntimeSupport
import KotlinStdlib

public func foo() -> any ExportedKotlinPackages.kotlin.collections.MutableList_Typed<Swift.String> {
    return ExportedKotlinPackages.kotlin.collections.MutableList_TypedImpl<Swift.String>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___foo(), conformsTo: ExportedKotlinPackages.kotlin.collections.MutableList.Type.self) as! any ExportedKotlinPackages.kotlin.collections.MutableList, conformsTo: { wrapperClass in wrapperClass is Swift.String })
}
public func testListAny(
    l: any ExportedKotlinPackages.kotlin.collections.List_Typed<any KotlinRuntimeSupport._KotlinBridgeable>
) -> any ExportedKotlinPackages.kotlin.collections.List_Typed<any KotlinRuntimeSupport._KotlinBridgeable> {
    return ExportedKotlinPackages.kotlin.collections.List_TypedImpl<any KotlinRuntimeSupport._KotlinBridgeable>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListAny__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_List_Typed_anyU20KotlinRuntimeSupport__KotlinBridgeable___(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: { wrapperClass in wrapperClass is any KotlinRuntimeSupport._KotlinBridgeable })
}
public func testListInt(
    l: any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Int32>
) -> any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Int32> {
    return ExportedKotlinPackages.kotlin.collections.List_TypedImpl<Swift.Int32>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListInt__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_List_Typed_Swift_Int32___(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: { wrapperClass in wrapperClass is Swift.Int32 })
}
public func testListListInt(
    l: any ExportedKotlinPackages.kotlin.collections.List_Typed<any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Int32>>
) -> any ExportedKotlinPackages.kotlin.collections.List_Typed<any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Int32>> {
    return ExportedKotlinPackages.kotlin.collections.List_TypedImpl<any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Int32>>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListListInt__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_List_Typed_anyU20ExportedKotlinPackages_kotlin_collections_List_Typed_Swift_Int32____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: { wrapperClass in wrapperClass is any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Int32> })
}
public func testListNothing(
    l: any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Never>
) -> any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Never> {
    return ExportedKotlinPackages.kotlin.collections.List_TypedImpl<Swift.Never>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListNothing__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_List_Typed_Swift_Never___(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: { wrapperClass in wrapperClass is Swift.Never })
}
public func testListOptAny(
    l: any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>
) -> any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>> {
    return ExportedKotlinPackages.kotlin.collections.List_TypedImpl<Swift.Optional<any KotlinRuntimeSupport._KotlinBridgeable>>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListOptAny__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_List_Typed_Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: { wrapperClass in wrapperClass is any KotlinRuntimeSupport._KotlinBridgeable })
}
public func testListOptInt(
    l: any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Optional<Swift.Int32>>
) -> any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Optional<Swift.Int32>> {
    return ExportedKotlinPackages.kotlin.collections.List_TypedImpl<Swift.Optional<Swift.Int32>>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListOptInt__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_List_Typed_Swift_Optional_Swift_Int32____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: { wrapperClass in wrapperClass is Swift.Int32 })
}
public func testListOptListInt(
    l: any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Optional<any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Int32>>>
) -> any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Optional<any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Int32>>> {
    return ExportedKotlinPackages.kotlin.collections.List_TypedImpl<Swift.Optional<any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Int32>>>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListOptListInt__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_List_Typed_Swift_Optional_anyU20ExportedKotlinPackages_kotlin_collections_List_Typed_Swift_Int32_____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: { wrapperClass in wrapperClass is any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Int32> })
}
public func testListOptNothing(
    l: any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Optional<Swift.Never>>
) -> any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Optional<Swift.Never>> {
    return ExportedKotlinPackages.kotlin.collections.List_TypedImpl<Swift.Optional<Swift.Never>>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListOptNothing__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_List_Typed_Swift_Optional_Swift_Never____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: { wrapperClass in wrapperClass is Swift.Never })
}
public func testListOptString(
    l: any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Optional<Swift.String>>
) -> any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Optional<Swift.String>> {
    return ExportedKotlinPackages.kotlin.collections.List_TypedImpl<Swift.Optional<Swift.String>>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListOptString__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_List_Typed_Swift_Optional_Swift_String____(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: { wrapperClass in wrapperClass is Swift.String })
}
public func testListShort(
    l: any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Int16>
) -> any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Int16> {
    return ExportedKotlinPackages.kotlin.collections.List_TypedImpl<Swift.Int16>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListShort__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_List_Typed_Swift_Int16___(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: { wrapperClass in wrapperClass is Swift.Int16 })
}
public func testListString(
    l: any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.String>
) -> any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.String> {
    return ExportedKotlinPackages.kotlin.collections.List_TypedImpl<Swift.String>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testListString__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_List_Typed_Swift_String___(l.__rawCollection.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: { wrapperClass in wrapperClass is Swift.String })
}
public func testOptListInt(
    l: (any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Int32>)?
) -> (any ExportedKotlinPackages.kotlin.collections.List_Typed<Swift.Int32>)? {
    return { switch __root___testOptListInt__TypesOfArguments__Swift_Optional_anyU20ExportedKotlinPackages_kotlin_collections_List_Typed_Swift_Int32____(l.map { it in it.__rawCollection.__externalRCRef() } ?? nil) { case nil: .none; case let res?: ExportedKotlinPackages.kotlin.collections.List_TypedImpl<Swift.Int32>(rawList: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res, conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List, conformsTo: { wrapperClass in wrapperClass is Swift.Int32 }); } }()
}
public func testStarList(
    l: any ExportedKotlinPackages.kotlin.collections.List
) -> any ExportedKotlinPackages.kotlin.collections.List {
    return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: __root___testStarList__TypesOfArguments__anyU20ExportedKotlinPackages_kotlin_collections_List__(l.__externalRCRef()), conformsTo: ExportedKotlinPackages.kotlin.collections.List.Type.self) as! any ExportedKotlinPackages.kotlin.collections.List
}
