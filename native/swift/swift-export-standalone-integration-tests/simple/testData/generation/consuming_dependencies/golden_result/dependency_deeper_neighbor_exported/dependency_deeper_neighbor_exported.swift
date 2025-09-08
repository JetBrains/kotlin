@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_dependency_deeper_neighbor_exported
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.dependency.four {
    public final class AnotherBar: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != ExportedKotlinPackages.dependency.four.AnotherBar.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.dependency.four.AnotherBar ") }
            let __kt = dependency_four_AnotherBar_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            dependency_four_AnotherBar_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
}
