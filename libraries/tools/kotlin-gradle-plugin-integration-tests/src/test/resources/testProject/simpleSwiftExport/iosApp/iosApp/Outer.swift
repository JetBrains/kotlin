import KotlinRuntime

public final class Outer: KotlinRuntime.KotlinBase {
    public final class Inner: KotlinRuntime.KotlinBase {
        public final class InnerInner: KotlinRuntime.KotlinBase {
            package override init(
                __externalRCRef: Swift.UInt
            ) {
                super.init(__externalRCRef: __externalRCRef)
            }
            public init(
                outer: InnerClass.Outer.Inner
            ) {
                let __kt = Outer_Inner_InnerInner_init_allocate()
                super.init(__externalRCRef: __kt)
                Outer_Inner_InnerInner_init_initialize__TypesOfArguments__Swift_UInt_InnerClass_Outer_Inner__(__kt, outer.__externalRCRef())
            }
            public func getOutPropertyFromInnerClass() -> Swift.Int32 {
                return Outer_Inner_InnerInner_getOutPropertyFromInnerClass(self.__externalRCRef())
            }
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public init(
            outer: InnerClass.Outer
        ) {
            let __kt = Outer_Inner_init_allocate()
            super.init(__externalRCRef: __kt)
            Outer_Inner_init_initialize__TypesOfArguments__Swift_UInt_InnerClass_Outer__(__kt, outer.__externalRCRef())
        }
        public func getOuterProperty() -> Swift.Int32 {
            return Outer_Inner_getOuterProperty(self.__externalRCRef())
        }
    }
    public var outerProperty: Swift.Int32 {
        get {
            return Outer_outerProperty_get(self.__externalRCRef())
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
