@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public final class AnyConsumer: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
}
public final class IdentityProcessor: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
}
public final class StringProducer: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
}
public func createMap(
    pairs: [Swift.Never]
) -> [KotlinRuntime.KotlinBase?: KotlinRuntime.KotlinBase?] {
    fatalError()
}
public func customFilter(
    _ receiver: [KotlinRuntime.KotlinBase?],
    predicate: @escaping (Swift.Optional<KotlinRuntime.KotlinBase>) -> Swift.Bool
) -> [KotlinRuntime.KotlinBase?] {
    return __root___customFilter__TypesOfArguments__Swift_Array_Swift_Optional_KotlinRuntime_KotlinBase___U28Swift_Optional_KotlinRuntime_KotlinBase_U29202D_U20Swift_Bool__(receiver.map { it in it as NSObject? ?? NSNull() }, {
        let originalBlock = predicate
        return { arg0 in return originalBlock({ switch arg0 { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()) }
    }()) as! Swift.Array<Swift.Optional<KotlinRuntime.KotlinBase>>
}
public func foo(
    param1: KotlinRuntime.KotlinBase?,
    param2: KotlinRuntime.KotlinBase?
) -> Swift.Void {
    return __root___foo__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase__Swift_Optional_KotlinRuntime_KotlinBase___(param1.map { it in it.__externalRCRef() } ?? nil, param2.map { it in it.__externalRCRef() } ?? nil)
}
