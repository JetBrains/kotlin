@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

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
public func function(
    arg: Swift.Int32
) -> Swift.Int32 {
    ExportedKotlinPackages.org.kotlin.foo.function(arg: arg)
}
public func getX(
    _ receiver: Swift.Int32
) -> Swift.String {
    ExportedKotlinPackages.org.kotlin.foo.getX(receiver)
}
public func y(
    _ receiver: Swift.String
) -> Swift.Int32 {
    ExportedKotlinPackages.org.kotlin.foo.y(receiver)
}
extension ExportedKotlinPackages.org.kotlin.foo.bar {
    public typealias Integer = Swift.Int32
}
extension ExportedKotlinPackages.org.kotlin.baz {
    public typealias Integer = Swift.Int32
}
extension ExportedKotlinPackages.org.kotlin.foo {
    public typealias Typealias = Swift.Int32
    public final class Clazz: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != ExportedKotlinPackages.org.kotlin.foo.Clazz.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from ExportedKotlinPackages.org.kotlin.foo.Clazz ") }
            let __kt = org_kotlin_foo_Clazz_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            org_kotlin_foo_Clazz_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
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
            return org_kotlin_foo_variable_set__TypesOfArguments__Swift_Int32__(newValue)
        }
    }
    public static func function(
        arg: Swift.Int32
    ) -> Swift.Int32 {
        return org_kotlin_foo_function__TypesOfArguments__Swift_Int32__(arg)
    }
    public static func getX(
        _ receiver: Swift.Int32
    ) -> Swift.String {
        return org_kotlin_foo_x_get__TypesOfArguments__Swift_Int32__(receiver)
    }
    public static func y(
        _ receiver: Swift.String
    ) -> Swift.Int32 {
        return org_kotlin_foo_y__TypesOfArguments__Swift_String__(receiver)
    }
}
