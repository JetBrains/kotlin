@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_feature
import KotlinRuntime
import KotlinRuntimeSupport

extension ExportedKotlinPackages.oh.my.kotlin {
    public final class FeatureA: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != ExportedKotlinPackages.oh.my.kotlin.FeatureA.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.oh.my.kotlin.FeatureA ") }
            let __kt = oh_my_kotlin_FeatureA_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            oh_my_kotlin_FeatureA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class FeatureB: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != ExportedKotlinPackages.oh.my.kotlin.FeatureB.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.oh.my.kotlin.FeatureB ") }
            let __kt = oh_my_kotlin_FeatureB_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            oh_my_kotlin_FeatureB_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
}
