@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.generation.any.any {
    public static func bar(
        arg: (any KotlinRuntimeSupport._KotlinBridgeable)?
    ) -> (any KotlinRuntimeSupport._KotlinBridgeable)? {
        return { switch generation_any_any_bar__TypesOfArguments__Swift_Optional_anyU20KotlinRuntimeSupport__KotlinBridgeable___(arg.map { it in it.intoRCRefUnsafe() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res) as! any KotlinRuntimeSupport._KotlinBridgeable; } }()
    }
    public static func foo(
        arg: any KotlinRuntimeSupport._KotlinBridgeable
    ) -> any KotlinRuntimeSupport._KotlinBridgeable {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: generation_any_any_foo__TypesOfArguments__anyU20KotlinRuntimeSupport__KotlinBridgeable__(arg.intoRCRefUnsafe())) as! any KotlinRuntimeSupport._KotlinBridgeable
    }
}
