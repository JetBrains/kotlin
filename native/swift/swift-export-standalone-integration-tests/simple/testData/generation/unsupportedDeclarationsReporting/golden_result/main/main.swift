@_exported import ExportedKotlinPackages
@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public final class Foo: KotlinRuntime.KotlinBase {
    public final class Inner: KotlinRuntime.KotlinBase {
        public init(
            outer__: main.Foo
        ) {
            if Self.self != main.Foo.Inner.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Foo.Inner ") }
            let __kt = Foo_Inner_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            Foo_Inner_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Foo__(__kt, outer__.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
    }
    public final class Nested: KotlinRuntime.KotlinBase {
        public init() {
            if Self.self != main.Foo.Nested.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Foo.Nested ") }
            let __kt = Foo_Nested_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            Foo_Nested_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public static func +(
            this: main.Foo.Nested,
            other: Swift.Int32
        ) -> main.Foo.Nested {
            this._plus(other: other)
        }
        public func _plus(
            other: Swift.Int32
        ) -> main.Foo.Nested {
            return main.Foo.Nested.__createClassWrapper(externalRCRef: Foo_Nested_plus__TypesOfArguments__Swift_Int32__(self.__externalRCRef(), other))
        }
    }
    public init() {
        if Self.self != main.Foo.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Foo ") }
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Foo_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
extension ExportedKotlinPackages.a.b.c {
    public enum E: KotlinRuntimeSupport._KotlinBridgeable, Swift.CaseIterable {
        case A
        case B
        case C
        public static var allCases: [ExportedKotlinPackages.a.b.c.E] {
            get {
                return a_b_c_E_entries_get() as! Swift.Array<ExportedKotlinPackages.a.b.c.E>
            }
        }
        public init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer!,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public static func valueOf(
            value: Swift.String
        ) -> ExportedKotlinPackages.a.b.c.E {
            return ExportedKotlinPackages.a.b.c.E.__createClassWrapper(externalRCRef: a_b_c_E_valueOf__TypesOfArguments__Swift_String__(value))
        }
    }
}
// Can't export foo: inline functions are not supported yet.
// Can't export foo: inline functions are not supported yet.
// Can't export a.b.c.E.values: static functions are not supported yet.
// Can't export a.b.c.E.values: static functions are not supported yet.
