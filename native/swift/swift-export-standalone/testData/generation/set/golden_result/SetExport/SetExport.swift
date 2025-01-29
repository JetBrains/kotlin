@_implementationOnly import KotlinBridges_SetExport
import KotlinRuntime
import KotlinRuntimeSupport

public func testOptSetInt(
    s: Swift.Set<Swift.Int32>?
) -> Swift.Set<Swift.Int32>? {
    return __root___testOptSetInt__TypesOfArguments__Swift_Set_Swift_Int32__opt___(s.map { it in Set(it.map { it in NSNumber(value: it) }) } ?? nil).map { it in it as! Swift.Set<Swift.Int32> }
}
public func testSetAny(
    s: Swift.Set<KotlinRuntime.KotlinBase>
) -> Swift.Set<KotlinRuntime.KotlinBase> {
    return __root___testSetAny__TypesOfArguments__Swift_Set_KotlinRuntime_KotlinBase___(s) as! Swift.Set<KotlinRuntime.KotlinBase>
}
public func testSetInt(
    s: Swift.Set<Swift.Int32>
) -> Swift.Set<Swift.Int32> {
    return __root___testSetInt__TypesOfArguments__Swift_Set_Swift_Int32___(Set(s.map { it in NSNumber(value: it) })) as! Swift.Set<Swift.Int32>
}
public func testSetListInt(
    s: Swift.Set<Swift.Array<Swift.Int32>>
) -> Swift.Set<Swift.Array<Swift.Int32>> {
    return __root___testSetListInt__TypesOfArguments__Swift_Set_Swift_Array_Swift_Int32____(Set(s.map { it in it.map { it in NSNumber(value: it) } })) as! Swift.Set<Swift.Array<Swift.Int32>>
}
public func testSetNothing(
    s: Swift.Set<Swift.Never>
) -> Swift.Set<Swift.Never> {
    return __root___testSetNothing__TypesOfArguments__Swift_Set_Swift_Never___(s) as! Swift.Set<Swift.Never>
}
public func testSetOptAny(
    s: Swift.Set<Swift.Optional<KotlinRuntime.KotlinBase>>
) -> Swift.Set<Swift.Optional<KotlinRuntime.KotlinBase>> {
    return __root___testSetOptAny__TypesOfArguments__Swift_Set_Swift_Optional_KotlinRuntime_KotlinBase____(Set(s.map { it in it as NSObject? ?? NSNull() })) as! Swift.Set<Swift.Optional<KotlinRuntime.KotlinBase>>
}
public func testSetOptInt(
    s: Swift.Set<Swift.Optional<Swift.Int32>>
) -> Swift.Set<Swift.Optional<Swift.Int32>> {
    return __root___testSetOptInt__TypesOfArguments__Swift_Set_Swift_Optional_Swift_Int32____(Set(s.map { it in it as NSObject? ?? NSNull() })) as! Swift.Set<Swift.Optional<Swift.Int32>>
}
public func testSetOptNothing(
    s: Swift.Set<Swift.Optional<Swift.Never>>
) -> Swift.Set<Swift.Optional<Swift.Never>> {
    return __root___testSetOptNothing__TypesOfArguments__Swift_Set_Swift_Optional_Swift_Never____(Set(s.map { it in it as NSObject? ?? NSNull() })) as! Swift.Set<Swift.Optional<Swift.Never>>
}
public func testSetOptSetInt(
    s: Swift.Set<Swift.Optional<Swift.Set<Swift.Int32>>>
) -> Swift.Set<Swift.Optional<Swift.Set<Swift.Int32>>> {
    return __root___testSetOptSetInt__TypesOfArguments__Swift_Set_Swift_Optional_Swift_Set_Swift_Int32_____(Set(s.map { it in it as NSObject? ?? NSNull() })) as! Swift.Set<Swift.Optional<Swift.Set<Swift.Int32>>>
}
public func testSetOptString(
    s: Swift.Set<Swift.Optional<Swift.String>>
) -> Swift.Set<Swift.Optional<Swift.String>> {
    return __root___testSetOptString__TypesOfArguments__Swift_Set_Swift_Optional_Swift_String____(Set(s.map { it in it as NSObject? ?? NSNull() })) as! Swift.Set<Swift.Optional<Swift.String>>
}
public func testSetSetInt(
    s: Swift.Set<Swift.Set<Swift.Int32>>
) -> Swift.Set<Swift.Set<Swift.Int32>> {
    return __root___testSetSetInt__TypesOfArguments__Swift_Set_Swift_Set_Swift_Int32____(Set(s.map { it in Set(it.map { it in NSNumber(value: it) }) })) as! Swift.Set<Swift.Set<Swift.Int32>>
}
public func testSetShort(
    s: Swift.Set<Swift.Int16>
) -> Swift.Set<Swift.Int16> {
    return __root___testSetShort__TypesOfArguments__Swift_Set_Swift_Int16___(Set(s.map { it in NSNumber(value: it) })) as! Swift.Set<Swift.Int16>
}
public func testSetString(
    s: Swift.Set<Swift.String>
) -> Swift.Set<Swift.String> {
    return __root___testSetString__TypesOfArguments__Swift_Set_Swift_String___(s) as! Swift.Set<Swift.String>
}
