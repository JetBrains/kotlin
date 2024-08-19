@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime

@_cdecl("SwiftExport_ExportedKotlinPackages_org_kotlin_Foo_toRetainedSwift")
private func SwiftExport_ExportedKotlinPackages_org_kotlin_Foo_toRetainedSwift(
    externalRCRef: Swift.UInt
) -> Swift.UnsafeMutableRawPointer {
    return Unmanaged.passRetained(ExportedKotlinPackages.org.kotlin.Foo(__externalRCRef: externalRCRef)).toOpaque()
}
public extension ExportedKotlinPackages.org.kotlin {
    public final class Foo : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = org_kotlin_Foo_init_allocate()
            super.init(__externalRCRef: __kt)
            org_kotlin_Foo_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}
