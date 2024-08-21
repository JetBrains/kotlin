@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime

public typealias ErrorDeprecatedClass = ExportedKotlinPackages.org.kotlin.foo.ErrorDeprecatedClass
public typealias ExampleClass = ExportedKotlinPackages.org.kotlin.foo.ExampleClass
public typealias HiddenDeprecatedClass = ExportedKotlinPackages.org.kotlin.foo.HiddenDeprecatedClass
@available(*, deprecated, obsoleted: 1.0, message: "Removed in Kotlin")
public var errorDeprecatedProperty: Swift.String {
    get {
        ExportedKotlinPackages.org.kotlin.foo.errorDeprecatedProperty
    }
}
@available(*, deprecated, obsoleted: 1.0, message: "Removed in Kotlin")
public var hiddenDeprecatedProperty: Swift.String {
    get {
        ExportedKotlinPackages.org.kotlin.foo.hiddenDeprecatedProperty
    }
}
@available(*, deprecated, obsoleted: 1.0, message: "Removed in Kotlin")
public func errorDeprecatedFunction() -> Swift.Void {
    ExportedKotlinPackages.org.kotlin.foo.errorDeprecatedFunction()
}
@available(*, deprecated, obsoleted: 1.0, message: "Removed in Kotlin")
public func hiddenDeprecatedFunction() -> Swift.Void {
    ExportedKotlinPackages.org.kotlin.foo.hiddenDeprecatedFunction()
}
public extension ExportedKotlinPackages.org.kotlin.foo {
    @available(*, deprecated, obsoleted: 1.0, message: "Removed in Kotlin")
    public final class ErrorDeprecatedClass : KotlinRuntime.KotlinBase {
        public override init() {
            fatalError("unavailable")
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public final class ExampleClass : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = org_kotlin_foo_ExampleClass_init_allocate()
            super.init(__externalRCRef: __kt)
            org_kotlin_foo_ExampleClass_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        @available(*, deprecated, obsoleted: 1.0, message: "Removed in Kotlin")
        public func errorDeprecatedMethod() -> Swift.Void {
            fatalError("unavailable")
        }
        @available(*, deprecated, obsoleted: 1.0, message: "Removed in Kotlin")
        public func hiddenDeprecatedMethod() -> Swift.Void {
            fatalError("unavailable")
        }
    }
    @available(*, deprecated, obsoleted: 1.0, message: "Removed in Kotlin")
    public final class HiddenDeprecatedClass : KotlinRuntime.KotlinBase {
        public override init() {
            fatalError("unavailable")
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    @available(*, deprecated, obsoleted: 1.0, message: "Removed in Kotlin")
    public static var errorDeprecatedProperty: Swift.String {
        get {
            fatalError("unavailable")
        }
    }
    @available(*, deprecated, obsoleted: 1.0, message: "Removed in Kotlin")
    public static var hiddenDeprecatedProperty: Swift.String {
        get {
            fatalError("unavailable")
        }
    }
    @available(*, deprecated, obsoleted: 1.0, message: "Removed in Kotlin")
    public static func errorDeprecatedFunction() -> Swift.Void {
        fatalError("unavailable")
    }
    @available(*, deprecated, obsoleted: 1.0, message: "Removed in Kotlin")
    public static func hiddenDeprecatedFunction() -> Swift.Void {
        fatalError("unavailable")
    }
}
