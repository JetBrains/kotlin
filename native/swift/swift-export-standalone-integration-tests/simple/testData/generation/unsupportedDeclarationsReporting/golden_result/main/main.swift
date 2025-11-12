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
    public enum E: KotlinRuntimeSupport._KotlinBridgeable, Swift.CaseIterable, Swift.LosslessStringConvertible, Swift.RawRepresentable {
        case A
        case B
        case C
        public var description: Swift.String {
            get {
                switch self {
                case .A: "A"
                case .B: "B"
                case .C: "C"
                default: fatalError()
                }
            }
        }
        public var rawValue: Swift.Int32 {
            get {
                switch self {
                case .A: 0
                case .B: 1
                case .C: 2
                default: fatalError()
                }
            }
        }
        public init?(
            _ description: Swift.String
        ) {
            switch description {
            case "A": self = .A
            case "B": self = .B
            case "C": self = .C
            default: return nil
            }
        }
        public init?(
            rawValue: Swift.Int32
        ) {
            guard 0..<3 ~= rawValue else { return nil }
            self = E.allCases[Int(rawValue)]
        }
        public init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer!,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            switch __externalRCRefUnsafe {
            case a_b_c_E_A(): self = .A
            case a_b_c_E_B(): self = .B
            case a_b_c_E_C(): self = .C
            default: fatalError()
            }
        }
        public func __externalRCRef() -> Swift.UnsafeMutableRawPointer! {
            return switch self {
            case .A: a_b_c_E_A()
            case .B: a_b_c_E_B()
            case .C: a_b_c_E_C()
            default: fatalError()
            }
        }
    }
}
// Can't export foo: inline functions are not supported yet.
// Can't export foo: inline functions are not supported yet.
// Can't export a.b.c.E.values: static functions are not supported yet.
// Can't export a.b.c.E.values: static functions are not supported yet.
