@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.generation.any.any {
    public static func bar(
        arg: Any?
    ) -> Any? {
        return { switch generation_any_any_bar__TypesOfArguments__Swift_Optional_Any___(arg.map { it in it.__externalRCRef() } ?? nil) { case nil: .none; case let res: KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: res) as! Any; } }()
    }
    public static func foo(
        arg: Any
    ) -> Any {
        return KotlinRuntime.KotlinBase.__createProtocolWrapper(externalRCRef: generation_any_any_foo__TypesOfArguments__Any__(arg.__externalRCRef())) as! Any
    }
}
