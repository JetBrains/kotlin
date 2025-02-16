@_implementationOnly import KotlinBridges_ListExport
import KotlinRuntime
import KotlinRuntimeSupport

public func testListAny(
    l: [KotlinRuntime.KotlinBase]
) -> [KotlinRuntime.KotlinBase] {
    return __root___testListAny__TypesOfArguments__Swift_Array_KotlinRuntime_KotlinBase___(l) as! Swift.Array<KotlinRuntime.KotlinBase>
}
public func testListInt(
    l: [Swift.Int32]
) -> [Swift.Int32] {
    return __root___testListInt__TypesOfArguments__Swift_Array_Swift_Int32___(l.map { it in NSNumber(value: it) }) as! Swift.Array<Swift.Int32>
}
public func testListListInt(
    l: [[Swift.Int32]]
) -> [[Swift.Int32]] {
    return __root___testListListInt__TypesOfArguments__Swift_Array_Swift_Array_Swift_Int32____(l.map { it in it.map { it in NSNumber(value: it) } }) as! Swift.Array<Swift.Array<Swift.Int32>>
}
public func testListNothing(
    l: [Swift.Never]
) -> [Swift.Never] {
    return __root___testListNothing__TypesOfArguments__Swift_Array_Swift_Never___(l) as! Swift.Array<Swift.Never>
}
public func testListOptAny(
    l: [KotlinRuntime.KotlinBase?]
) -> [KotlinRuntime.KotlinBase?] {
    return __root___testListOptAny__TypesOfArguments__Swift_Array_Swift_Optional_KotlinRuntime_KotlinBase____(l.map { it in it as NSObject? ?? NSNull() }) as! Swift.Array<Swift.Optional<KotlinRuntime.KotlinBase>>
}
public func testListOptInt(
    l: [Swift.Int32?]
) -> [Swift.Int32?] {
    return __root___testListOptInt__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Int32____(l.map { it in it as NSObject? ?? NSNull() }) as! Swift.Array<Swift.Optional<Swift.Int32>>
}
public func testListOptListInt(
    l: [[Swift.Int32]?]
) -> [[Swift.Int32]?] {
    return __root___testListOptListInt__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Array_Swift_Int32_____(l.map { it in it as NSObject? ?? NSNull() }) as! Swift.Array<Swift.Optional<Swift.Array<Swift.Int32>>>
}
public func testListOptNothing(
    l: [Swift.Never?]
) -> [Swift.Never?] {
    return __root___testListOptNothing__TypesOfArguments__Swift_Array_Swift_Optional_Swift_Never____(l.map { it in it as NSObject? ?? NSNull() }) as! Swift.Array<Swift.Optional<Swift.Never>>
}
public func testListOptString(
    l: [Swift.String?]
) -> [Swift.String?] {
    return __root___testListOptString__TypesOfArguments__Swift_Array_Swift_Optional_Swift_String____(l.map { it in it as NSObject? ?? NSNull() }) as! Swift.Array<Swift.Optional<Swift.String>>
}
public func testListShort(
    l: [Swift.Int16]
) -> [Swift.Int16] {
    return __root___testListShort__TypesOfArguments__Swift_Array_Swift_Int16___(l.map { it in NSNumber(value: it) }) as! Swift.Array<Swift.Int16>
}
public func testListString(
    l: [Swift.String]
) -> [Swift.String] {
    return __root___testListString__TypesOfArguments__Swift_Array_Swift_String___(l) as! Swift.Array<Swift.String>
}
public func testOptListInt(
    l: [Swift.Int32]?
) -> [Swift.Int32]? {
    return __root___testOptListInt__TypesOfArguments__Swift_Array_Swift_Int32__opt___(l.map { it in it.map { it in NSNumber(value: it) } } ?? nil).map { it in it as! Swift.Array<Swift.Int32> }
}
