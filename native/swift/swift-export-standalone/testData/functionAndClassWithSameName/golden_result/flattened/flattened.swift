@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_flattened
import KotlinRuntime

public typealias FlattenedPackageClass = ExportedKotlinPackages.flattenedPackage.FlattenedPackageClass
public func flattenedPackageClass(
    i: Swift.Int32
) -> ExportedKotlinPackages.flattenedPackage.FlattenedPackageClass {
    ExportedKotlinPackages.flattenedPackage.flattenedPackageClass(i: i)
}
public extension ExportedKotlinPackages.flattenedPackage {
    public final class FlattenedPackageClass : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = flattenedPackage_FlattenedPackageClass_init_allocate()
            super.init(__externalRCRef: __kt)
            flattenedPackage_FlattenedPackageClass_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public static func flattenedPackageClass(
        i: Swift.Int32
    ) -> ExportedKotlinPackages.flattenedPackage.FlattenedPackageClass {
        return ExportedKotlinPackages.flattenedPackage.FlattenedPackageClass(__externalRCRef: flattenedPackage_FlattenedPackageClass__TypesOfArguments__int32_t__(i))
    }
}
