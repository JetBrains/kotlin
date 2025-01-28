@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_common
import modulea

public typealias demo = ExportedKotlinPackages.namespace.demo
public extension ExportedKotlinPackages.namespace.demo {
    public static func useClassFromA() -> ExportedKotlinPackages.namespace.modulea.ClassFromA {
        return ExportedKotlinPackages.namespace.modulea.ClassFromA(__externalRCRef: namespace_demo_useClassFromA())
    }
}
