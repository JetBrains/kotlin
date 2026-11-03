@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public func bar(
    arg: Any?
) -> Any? {
    return { switch __root___bar__TypesOfArguments__Swift_Optional_Any___(arg.map { it in KotlinRuntime.KotlinBase.__externalRCRefForAny(it) } ?? nil) { case nil: .none; case let res?: KotlinRuntime.KotlinBase.__createBridgeableAny(externalRCRef: res); } }()
}
public func foo(
    arg: Any
) -> Any {
    return KotlinRuntime.KotlinBase.__createBridgeableAny(externalRCRef: __root___foo__TypesOfArguments__Any__(KotlinRuntime.KotlinBase.__externalRCRefForAny(arg)))
}
