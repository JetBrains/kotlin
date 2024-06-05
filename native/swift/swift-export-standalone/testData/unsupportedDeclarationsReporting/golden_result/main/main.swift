import KotlinBridges_main
import KotlinRuntime

public class Foo : KotlinRuntime.KotlinBase {
    public class Nested : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = Foo_Nested_init_allocate()
            super.init(__externalRCRef: __kt)
            Foo_Nested_init_initialize__TypesOfArguments__uintptr_t__(__kt)
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
        __root___Foo_init_initialize__TypesOfArguments__uintptr_t__(__kt)
    }
    public override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
// Can't export ext: extension functions are not supported yet.
// Can't export foo: inline functions are not supported yet.
// Can't export MyInterface: interface classifiers are not supported yet.
// Can't export a.b.c.A: non-final classes are not supported yet.
// Can't export a.b.c.E: enum_class classifiers are not supported yet.
// Can't export Foo.Inner: inner classes are not supported yet.
// Can't export Foo.Nested.plus: operators are not supported yet.
