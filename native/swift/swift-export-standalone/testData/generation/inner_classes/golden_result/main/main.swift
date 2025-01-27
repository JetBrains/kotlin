@_implementationOnly import KotlinBridges_main
import KotlinRuntime

public final class Outer: KotlinRuntime.KotlinBase {
    public final class Inner: KotlinRuntime.KotlinBase {
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public init(
            outer: main.Outer
        ) {
            let __kt = Outer_Inner_init_allocate()
            super.init(__externalRCRef: __kt)
            Outer_Inner_init_initialize__TypesOfArguments__Swift_UInt_main_Outer__(__kt, outer.__externalRCRef())
        }
        public func foo() -> Swift.Int32 {
            return Outer_Inner_foo(self.__externalRCRef())
        }
    }
    public override init() {
        let __kt = __root___Outer_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Outer_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
