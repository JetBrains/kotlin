@_exported import ExportedKotlinPackages
import KotlinRuntimeSupport
import KotlinRuntime
@_implementationOnly import KotlinBridges_testLibraryA

public extension ExportedKotlinPackages.org.jetbrains.a {
    public final class MyLibraryA: KotlinRuntime.KotlinBase, KotlinRuntimeSupport._KotlinBridged {
        public func returnInt() -> Swift.Int32 {
            return org_jetbrains_a_MyLibraryA_returnInt(self.__externalRCRef())
        }
        public func returnMe() -> ExportedKotlinPackages.org.jetbrains.a.MyLibraryA {
            return ExportedKotlinPackages.org.jetbrains.a.MyLibraryA.__create(externalRCRef: org_jetbrains_a_MyLibraryA_returnMe(self.__externalRCRef()))
        }
        public init() {
            precondition(Self.self == ExportedKotlinPackages.org.jetbrains.a.MyLibraryA.self, "Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.org.jetbrains.a.MyLibraryA ")
            let __kt = org_jetbrains_a_MyLibraryA_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            org_jetbrains_a_MyLibraryA_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
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
