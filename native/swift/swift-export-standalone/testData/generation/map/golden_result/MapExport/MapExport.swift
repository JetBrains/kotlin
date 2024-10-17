@_implementationOnly import KotlinBridges_MapExport
import KotlinRuntime

public func testMapAnyLong(
    m: [KotlinRuntime.KotlinBase: Swift.Int64]
) -> [KotlinRuntime.KotlinBase: Swift.Int64] {
    return __root___testMapAnyLong__TypesOfArguments__Swift_Dictionary_KotlinRuntime_KotlinBase_Swift_Int64___(m) as! Swift.Dictionary<KotlinRuntime.KotlinBase,Swift.Int64>
}
public func testMapIntString(
    m: [Swift.Int32: Swift.String]
) -> [Swift.Int32: Swift.String] {
    return __root___testMapIntString__TypesOfArguments__Swift_Dictionary_Swift_Int32_Swift_String___(m) as! Swift.Dictionary<Swift.Int32,Swift.String>
}
public func testMapListIntSetInt(
    m: [[Swift.Int32]: Swift.Set<Swift.Int32>]
) -> [[Swift.Int32]: Swift.Set<Swift.Int32>] {
    return __root___testMapListIntSetInt__TypesOfArguments__Swift_Dictionary_Swift_Array_Swift_Int32__Swift_Set_Swift_Int32____(m) as! Swift.Dictionary<Swift.Array<Swift.Int32>,Swift.Set<Swift.Int32>>
}
public func testMapLongAny(
    m: [Swift.Int64: KotlinRuntime.KotlinBase]
) -> [Swift.Int64: KotlinRuntime.KotlinBase] {
    return __root___testMapLongAny__TypesOfArguments__Swift_Dictionary_Swift_Int64_KotlinRuntime_KotlinBase___(m) as! Swift.Dictionary<Swift.Int64,KotlinRuntime.KotlinBase>
}
public func testMapNothingOptNothing(
    m: [Swift.Never: Swift.Never?]
) -> [Swift.Never: Swift.Never?] {
    return __root___testMapNothingOptNothing__TypesOfArguments__Swift_Dictionary_Swift_Never_Swift_Optional_Swift_Never____(m) as! Swift.Dictionary<Swift.Never,Swift.Optional<Swift.Never>>
}
public func testMapOptIntListInt(
    m: Swift.Never
) -> Swift.Never {
    fatalError()
}
public func testMapOptNothingNothing(
    m: Swift.Never
) -> Swift.Never {
    fatalError()
}
public func testMapSetIntMapIntInt(
    m: [Swift.Set<Swift.Int32>: [Swift.Int32: Swift.Int32]]
) -> [Swift.Set<Swift.Int32>: [Swift.Int32: Swift.Int32]] {
    return __root___testMapSetIntMapIntInt__TypesOfArguments__Swift_Dictionary_Swift_Set_Swift_Int32__Swift_Dictionary_Swift_Int32_Swift_Int32____(m) as! Swift.Dictionary<Swift.Set<Swift.Int32>,Swift.Dictionary<Swift.Int32,Swift.Int32>>
}
public func testMapStringInt(
    m: [Swift.String: Swift.Int32]
) -> [Swift.String: Swift.Int32] {
    return __root___testMapStringInt__TypesOfArguments__Swift_Dictionary_Swift_String_Swift_Int32___(m) as! Swift.Dictionary<Swift.String,Swift.Int32>
}
