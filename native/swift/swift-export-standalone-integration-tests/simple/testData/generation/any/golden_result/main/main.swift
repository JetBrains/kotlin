@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public func bar(
    arg: (any KotlinRuntimeSupport._KotlinBridgeable)?
) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
    return { switch __root___bar__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(arg.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: res); } }()
}
public func foo(
    arg: any KotlinRuntimeSupport._KotlinBridgeable
) -> any KotlinRuntimeSupport._KotlinBridgeable {
    return KotlinRuntime.KotlinBase.__createBridgeable(externalRCRef: __root___foo__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(arg.__externalRCRef()))
}
