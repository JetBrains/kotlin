@_implementationOnly import KotlinBridges_main
import KotlinRuntime

public final class Foo: KotlinRuntime.KotlinBase {
    public final class Nested: KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = Foo_Nested_init_allocate()
            super.init(__externalRCRef: __kt)
            Foo_Nested_init_initialize__TypesOfArguments__Swift_UInt__(__kt)
        }
        package override init(
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
    package override init(
        __externalRCRef: Swift.UInt
    ) {
        super.init(__externalRCRef: __externalRCRef)
    }
}
public extension main.a.b.c {
    public final class E: KotlinRuntime.KotlinBase, Swift.CaseIterable {
        public static var A: main.a.b.c.E {
            get {
                return main.a.b.c.E(__externalRCRef: a_b_c_E_A_get())
            }
        }
        public static var B: main.a.b.c.E {
            get {
                return main.a.b.c.E(__externalRCRef: a_b_c_E_B_get())
            }
        }
        public static var C: main.a.b.c.E {
            get {
                return main.a.b.c.E(__externalRCRef: a_b_c_E_C_get())
            }
        }
        public static var allCases: [main.a.b.c.E] {
            get {
                return a_b_c_E_entries_get() as! Swift.Array<main.a.b.c.E>
            }
        }
        package override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
        public static func valueOf(
            value: Swift.String
        ) -> main.a.b.c.E {
            return main.a.b.c.E(__externalRCRef: a_b_c_E_valueOf__TypesOfArguments__Swift_String__(value))
        }
    }
}
public enum a {
    public enum b {
        public enum c {
        }
    }
}
// Can't export extProp: extension properties are not supported yet.
// Can't export foo: inline functions are not supported yet.
// Can't export MyInterface: interface classifiers are not supported yet.
// Can't export Foo.extPropMember: extension properties are not supported yet.
// Can't export Foo.Inner: inner classes are not supported yet.
// Can't export Foo.Nested.plus: operators are not supported yet.
// Can't export a.b.c.E.values: static functions are not supported yet.
