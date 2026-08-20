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
            let __kt = flattenedPackage_FlattenedPackageClass_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { flattenedPackage_FlattenedPackageClass_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public static func flattenedPackageClass(
        i: Swift.Int32
    ) -> ExportedKotlinPackages.flattenedPackage.FlattenedPackageClass {
        return ExportedKotlinPackages.flattenedPackage.FlattenedPackageClass.__createClassWrapper(externalRCRef: flattenedPackage_FlattenedPackageClass__TypesOfArguments__Swift_Int32__(i))
    }
}
extension ExportedKotlinPackages.test.factory.suffix {
    public enum Foo_SealedType: KotlinRuntimeSupport.SealedType {
        case basicFoo(ExportedKotlinPackages.test.factory.suffix.BasicFoo_SealedType)
        public var value: ExportedKotlinPackages.test.factory.suffix.Foo {
            get {
                switch self {
                case let .basicFoo(type): type.value
                }
            }
        }
    }
    public final class BasicFoo: ExportedKotlinPackages.test.factory.suffix.Foo {
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        public override func sealedType() -> ExportedKotlinPackages.test.factory.suffix.Foo_SealedType {
            .basicFoo(.init(self))
        }
    }
    open class Foo: KotlinRuntime.KotlinBase {
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
        open func sealedType() -> ExportedKotlinPackages.test.factory.suffix.Foo_SealedType {
            fatalError("must implement sealedType in subclass")
        }
    }
    public struct BasicFoo_SealedType: KotlinRuntimeSupport.SealedType {
        public let value: ExportedKotlinPackages.test.factory.suffix.BasicFoo
        init(
            _ value: ExportedKotlinPackages.test.factory.suffix.BasicFoo
        ) {
            self.value = value
        }
    }
    public static func basicFoo() -> ExportedKotlinPackages.test.factory.suffix.Foo {
        return ExportedKotlinPackages.test.factory.suffix.Foo.__createClassWrapper(externalRCRef: test_factory_suffix_BasicFoo())
    }
}
