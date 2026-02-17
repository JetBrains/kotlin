@_implementationOnly import KotlinBridges_receivers
import KotlinRuntime
import KotlinRuntimeSupport

public func foo(
    i: @escaping (Swift.Int32) -> Swift.Void
) -> Swift.Void {
    return __root___foo__TypesOfArguments__U28Swift_Int32U29202D_U20Swift_Void__({
        let originalBlock = i
        return { arg0 in return originalBlock(arg0) }
    }())
}
public func fooAny(
    i: @escaping (any KotlinRuntimeSupport._KotlinBridgeable) -> Swift.Void
) -> Swift.Void {
    return __root___fooAny__TypesOfArguments__U28anyU20KotlinRuntimeSupport__KotlinBridgeableU29202D_U20Swift_Void__({
        let originalBlock = i
        return { arg0 in return originalBlock(KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: arg0)) }
    }())
}
public func fooList(
    i: @escaping ([Swift.Int32]) -> Swift.Void
) -> Swift.Void {
    return __root___fooList__TypesOfArguments__U28Swift_Array_Swift_Int32_U29202D_U20Swift_Void__({
        let originalBlock = i
        return { arg0 in return originalBlock(arg0 as! Swift.Array<Swift.Int32>) }
    }())
}
public func fooString(
    i: @escaping (Swift.String?) -> Swift.Void
) -> Swift.Void {
    return __root___fooString__TypesOfArguments__U28Swift_Optional_Swift_String_U29202D_U20Swift_Void__({
        let originalBlock = i
        return { arg0 in return originalBlock(arg0) }
    }())
}
