@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime

public typealias Clazz = ExportedKotlinPackages.org.kotlin.foo.Clazz
public typealias Typealias = ExportedKotlinPackages.org.kotlin.foo.Typealias
public typealias bar = ExportedKotlinPackages.org.kotlin.foo.bar
public var constant: Swift.Int32 {
    get {
        ExportedKotlinPackages.org.kotlin.foo.constant
    }
}
public var variable: Swift.Int32 {
    get {
        ExportedKotlinPackages.org.kotlin.foo.variable
    }
    set {
        ExportedKotlinPackages.org.kotlin.foo.variable = newValue
    }
}
@_cdecl("SwiftExport_ExportedKotlinPackages_org_kotlin_foo_Clazz_toRetainedSwift")
private func SwiftExport_ExportedKotlinPackages_org_kotlin_foo_Clazz_toRetainedSwift(
    externalRCRef: Swift.UInt
) -> Swift.UnsafeMutableRawPointer {
    return Unmanaged.passRetained(ExportedKotlinPackages.org.kotlin.foo.Clazz(__externalRCRef: externalRCRef)).toOpaque()
}
public func function(
    arg: Swift.Int32
) -> Swift.Int32 {
    ExportedKotlinPackages.org.kotlin.foo.function(arg: arg)
}
public extension ExportedKotlinPackages.org.kotlin.foo.bar {
    public typealias Integer = Swift.Int32
}
public extension ExportedKotlinPackages.org.kotlin.baz {
    public typealias Integer = Swift.Int32
}
public extension ExportedKotlinPackages.org.kotlin.foo {
    public typealias Typealias = Swift.Int32
    public final class Clazz : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = org_kotlin_foo_Clazz_init_allocate()
            super.init(__externalRCRef: __kt)
            org_kotlin_foo_Clazz_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public static var constant: Swift.Int32 {
        get {
            return org_kotlin_foo_constant_get()
        }
    }
    public static var variable: Swift.Int32 {
        get {
            return org_kotlin_foo_variable_get()
        }
        set {
            return org_kotlin_foo_variable_set__TypesOfArguments__int32_t__(newValue)
        }
    }
    public static func function(
        arg: Swift.Int32
    ) -> Swift.Int32 {
        return org_kotlin_foo_function__TypesOfArguments__int32_t__(arg)
    }
}
