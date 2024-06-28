@_exported import ExportedKotlinPackages
import KotlinBridges_main
import KotlinRuntime

public extension ExportedKotlinPackages.org.kotlin {
    public class Foo : KotlinRuntime.KotlinBase {
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
