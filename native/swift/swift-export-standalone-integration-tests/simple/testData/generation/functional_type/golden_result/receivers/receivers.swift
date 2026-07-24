@_implementationOnly import KotlinBridges_receivers
import KotlinRuntime
import KotlinRuntimeSupport

public func foo(
    i: @escaping (Swift.Int32) -> Swift.Void
) -> Swift.Void {
    return { __root___foo__TypesOfArguments__U28Swift_Int32U29202D_U20Swift_Void__({
        let originalBlock: (Swift.Int32) -> Swift.Void = i
        return { (arg0: Swift.Int32) in
            let _arg0: Swift.Int32 = arg0
            let _result = originalBlock(_arg0)
            return { _result; return true }()
        }
    }()); return () }()
}
public func fooAny(
    i: @escaping (any KotlinRuntimeSupport._KotlinBridgeable) -> Swift.Void
) -> Swift.Void {
    return { __root___fooAny__TypesOfArguments__U28anyU20KotlinRuntimeSupport__KotlinBridgeableU29202D_U20Swift_Void__({
        let originalBlock: (any KotlinRuntimeSupport._KotlinBridgeable) -> Swift.Void = i
        return { (arg0: Swift.UnsafeMutableRawPointer) in
            let _arg0: any KotlinRuntimeSupport._KotlinBridgeable = KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: arg0)
            let _result = originalBlock(_arg0)
            return { _result; return true }()
        }
    }()); return () }()
}
public func fooList(
    i: @escaping ([Swift.Int32]) -> Swift.Void
) -> Swift.Void {
    return { __root___fooList__TypesOfArguments__U28Swift_Array_Swift_Int32_U29202D_U20Swift_Void__({
        let originalBlock: (Swift.Array<Swift.Int32>) -> Swift.Void = i
        return { (arg0: Any) in
            let _arg0: Swift.Array<Swift.Int32> = arg0 as! Swift.Array<Swift.Int32>
            let _result = originalBlock(_arg0)
            return { _result; return true }()
        }
    }()); return () }()
}
public func fooString(
    i: @escaping (Swift.String?) -> Swift.Void
) -> Swift.Void {
    return { __root___fooString__TypesOfArguments__U28Swift_Optional_Swift_String_U29202D_U20Swift_Void__({
        let originalBlock: (Swift.Optional<Swift.String>) -> Swift.Void = i
        return { (arg0: Swift.String?) in
            let _arg0: Swift.Optional<Swift.String> = arg0
            let _result = originalBlock(_arg0)
            return { _result; return true }()
        }
    }()); return () }()
}
