@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.generation.any.any {
    public static func bar(
        arg: KotlinRuntime.KotlinBase?
    ) -> KotlinRuntime.KotlinBase? {
        return { switch generation_any_any_bar__TypesOfArguments__Swift_Optional_KotlinRuntime_KotlinBase___(arg.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: res); } }()
    }
    public static func foo(
        arg: KotlinRuntime.KotlinBase
    ) -> KotlinRuntime.KotlinBase {
        return KotlinRuntime.KotlinBase.__createClassWrapper(externalRCRef: generation_any_any_foo__TypesOfArguments__KotlinRuntime_KotlinBase__(arg.__externalRCRef()))
    }
}
