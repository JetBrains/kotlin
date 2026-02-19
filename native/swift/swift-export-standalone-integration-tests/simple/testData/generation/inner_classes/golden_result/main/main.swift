@_implementationOnly import KotlinBridges_main
import KotlinRuntime
import KotlinRuntimeSupport

public final class Outer: KotlinRuntime.KotlinBase {
    public final class Inner: KotlinRuntime.KotlinBase {
        public final class InnerInner: KotlinRuntime.KotlinBase {
            public init(
                outer__: main.Outer.Inner
            ) {
                if Self.self != main.Outer.Inner.InnerInner.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Outer.Inner.InnerInner ") }
                let __kt = Outer_Inner_InnerInner_init_allocate()
                super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
                Outer_Inner_InnerInner_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Outer_Inner__(__kt, outer__.__externalRCRef())
            }
            package override init(
                __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
                options: KotlinRuntime.KotlinBaseConstructionOptions
            ) {
                super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
            }
        }
        public init(
            outer__: main.Outer
        ) {
            if Self.self != main.Outer.Inner.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Outer.Inner ") }
            let __kt = Outer_Inner_init_allocate()
            super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
            Outer_Inner_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer_main_Outer__(__kt, outer__.__externalRCRef())
        }
        package override init(
            __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
            options: KotlinRuntime.KotlinBaseConstructionOptions
        ) {
            super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
        }
        public func foo() -> Swift.Int32 {
            return Outer_Inner_foo(self.__externalRCRef())
        }
    }
    public init() {
        if Self.self != main.Outer.self { fatalError("Inheritance from exported Kotlin classes is not supported yet: \(String(reflecting: Self.self)) inherits from main.Outer ") }
        let __kt = __root___Outer_init_allocate()
        super.init(__externalRCRefUnsafe: __kt, options: .asBoundBridge)
        __root___Outer_init_initialize__TypesOfArguments__Swift_UnsafeMutableRawPointer__(__kt)
    }
    package override init(
        __externalRCRefUnsafe: Swift.UnsafeMutableRawPointer?,
        options: KotlinRuntime.KotlinBaseConstructionOptions
    ) {
        super.init(__externalRCRefUnsafe: __externalRCRefUnsafe, options: options)
    }
}
