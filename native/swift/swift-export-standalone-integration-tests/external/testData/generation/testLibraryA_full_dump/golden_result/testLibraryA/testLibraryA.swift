@_exported import ExportedKotlinPackages
import KotlinRuntimeSupport
@_implementationOnly import KotlinBridges_testLibraryA
import KotlinRuntime

public extension ExportedKotlinPackages.org.jetbrains.a {
    public final class MyLibraryA: KotlinRuntime.KotlinBase {
        public func returnInt() -> Swift.Int32 {
            return org_jetbrains_a_MyLibraryA_returnInt(self.__externalRCRef())
        }
        public func returnMe() -> ExportedKotlinPackages.org.jetbrains.a.MyLibraryA {
            return ExportedKotlinPackages.org.jetbrains.a.MyLibraryA(__externalRCRef: org_jetbrains_a_MyLibraryA_returnMe(self.__externalRCRef()))
        }
        public override init() {
            let __kt = org_jetbrains_a_MyLibraryA_init_allocate()
            super.init(__externalRCRef: __kt)
            org_jetbrains_a_MyLibraryA_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public static var topLevelProperty: Swift.Int32 {
        get {
            return org_jetbrains_a_topLevelProperty_get()
        }
    }
    public static func topLevelFunction() -> Swift.Int32 {
        return org_jetbrains_a_topLevelFunction()
    }
}
