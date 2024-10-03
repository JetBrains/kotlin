@_implementationOnly import KotlinBridges_main
import KotlinRuntime

public final class Foo : KotlinRuntime.KotlinBase {
    public final class Nested : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = Foo_Nested_init_allocate()
            super.init(__externalRCRef: __kt)
            Foo_Nested_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public override init() {
        let __kt = __root___Foo_init_allocate()
        super.init(__externalRCRef: __kt)
        __root___Foo_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
extension main.Foo {
    public var extProp: Swift.Void {
        get {
            return __root___extProp_get__TypesOfArguments__main_Foo__(self.__externalRCRef())
        }
    }
}
extension main.Foo {
    public func ext() -> Swift.Void {
        return __root___ext__TypesOfArguments__main_Foo__(self.__externalRCRef())
    }
}
// Can't export foo: inline functions are not supported yet.
// Can't export MyInterface: interface classifiers are not supported yet.
// Can't export a.b.c.A: abstract classes are not supported yet.
// Can't export a.b.c.E: enum_class classifiers are not supported yet.
// Can't export Foo.extFunMember: member extension functions are not supported yet.
// Can't export Foo.extPropMember: member extension properties are not supported yet.
// Can't export Foo.Inner: inner classes are not supported yet.
// Can't export Foo.Nested.plus: operators are not supported yet.
