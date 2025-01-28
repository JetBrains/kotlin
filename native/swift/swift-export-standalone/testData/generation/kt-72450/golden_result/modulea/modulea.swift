@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_modulea
import KotlinRuntime

public typealias ClassFromA = ExportedKotlinPackages.namespace.modulea.ClassFromA
public extension ExportedKotlinPackages.namespace.modulea {
    public final class ClassFromA: KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = namespace_modulea_ClassFromA_init_allocate()
            super.init(__externalRCRef: __kt)
            namespace_modulea_ClassFromA_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}
