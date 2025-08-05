@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_flattened
import KotlinRuntime
import KotlinRuntimeSupport

public typealias FlattenedPackageClass = ExportedKotlinPackages.flattenedPackage.FlattenedPackageClass
public func flattenedPackageClass(
    i: Swift.Int32
) -> ExportedKotlinPackages.flattenedPackage.FlattenedPackageClass {
    ExportedKotlinPackages.flattenedPackage.flattenedPackageClass(i: i)
}
extension ExportedKotlinPackages.flattenedPackage {
    public final class FlattenedPackageClass: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != ExportedKotlinPackages.flattenedPackage.FlattenedPackageClass.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.flattenedPackage.FlattenedPackageClass ") }
            let __kt = flattenedPackage_FlattenedPackageClass_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            flattenedPackage_FlattenedPackageClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public static func flattenedPackageClass(
        i: Swift.Int32
    ) -> ExportedKotlinPackages.flattenedPackage.FlattenedPackageClass {
        return ExportedKotlinPackages.flattenedPackage.FlattenedPackageClass.__createClassWrapper(externalRCRef: flattenedPackage_FlattenedPackageClass__TypesOfArguments__Swift_Int32__(i))
    }
}
