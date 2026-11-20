@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public typealias Clazz = ExportedKotlinPackages.org.kotlin.foo.Clazz
public var variable: Swift.Int32 {
    get {
        ExportedKotlinPackages.org.kotlin.foo.variable
    }
    set {
        ExportedKotlinPackages.org.kotlin.foo.variable = newValue
    }
}
public func checkable() -> Swift.Int32 {
    return ExportedKotlinPackages.org.kotlin.foo.checkable()
}
public func discardable() -> Swift.Int32 {
    return ExportedKotlinPackages.org.kotlin.foo.discardable()
}
@discardableResult
public func explicitlyIgnorable() -> Swift.Int32 {
    return ExportedKotlinPackages.org.kotlin.foo.explicitlyIgnorable()
}
@discardableResult
public func ignorableInMarkedFile() -> Swift.Int32 {
    return ExportedKotlinPackages.org.kotlin.foo.ignorableInMarkedFile()
}
public func nothingFunction() -> Swift.Never {
    return ExportedKotlinPackages.org.kotlin.foo.nothingFunction()
}
public func unitFunction() -> Swift.Void {
    return ExportedKotlinPackages.org.kotlin.foo.unitFunction()
}
extension Swift.Int32 {
    public var extensionProperty: Self {
        get {
            let receiver = self
            return ExportedKotlinPackages.org.kotlin.foo.getExtensionProperty(receiver)
        }
    }
}
extension Swift.Int32 {
    public func discardableExtension() -> Self {
        let receiver = self
        return ExportedKotlinPackages.org.kotlin.foo.discardableExtension(receiver)
    }
}
extension Swift.Int32 {
    public func checkableExtension() -> Self {
        let receiver = self
        return ExportedKotlinPackages.org.kotlin.foo.checkableExtension(receiver)
    }
}
extension ExportedKotlinPackages.org.kotlin.bar {
    public static func checkableOutsideOfRoot() -> Swift.Int32 {
        return org_kotlin_bar_checkableOutsideOfRoot()
    }
    public static func discardableOutsideOfRoot() -> Swift.Int32 {
        return org_kotlin_bar_discardableOutsideOfRoot()
    }
}
extension ExportedKotlinPackages.org.kotlin.foo {
    public final class Clazz: KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = org_kotlin_foo_Clazz_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge);
            { org_kotlin_foo_Clazz_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt); return () }()
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options);
        }
    }
    public static var variable: Swift.Int32 {
        get {
            return org_kotlin_foo_variable_get()
        }
        set {
            return { org_kotlin_foo_variable_set__TypesOfArguments__Swift_Int32__(newValue); return () }()
        }
    }
    public static func checkable() -> Swift.Int32 {
        return org_kotlin_foo_checkable()
    }
    public static func checkableExtension(
        _ receiver: Swift.Int32
    ) -> Swift.Int32 {
        return org_kotlin_foo_checkableExtension__TypesOfArgumentsE__Swift_Int32__(receiver)
    }
    public static func discardable() -> Swift.Int32 {
        return org_kotlin_foo_discardable()
    }
    public static func discardableExtension(
        _ receiver: Swift.Int32
    ) -> Swift.Int32 {
        return org_kotlin_foo_discardableExtension__TypesOfArgumentsE__Swift_Int32__(receiver)
    }
    @discardableResult
    public static func explicitlyIgnorable() -> Swift.Int32 {
        return org_kotlin_foo_explicitlyIgnorable()
    }
    public static func getExtensionProperty(
        _ receiver: Swift.Int32
    ) -> Swift.Int32 {
        return org_kotlin_foo_extensionProperty_get__TypesOfArgumentsE__Swift_Int32__(receiver)
    }
    @discardableResult
    public static func ignorableInMarkedFile() -> Swift.Int32 {
        return org_kotlin_foo_ignorableInMarkedFile()
    }
    public static func nothingFunction() -> Swift.Never {
        return { org_kotlin_foo_nothingFunction(); fatalError() }()
    }
    public static func unitFunction() -> Swift.Void {
        return { org_kotlin_foo_unitFunction(); return () }()
    }
}
