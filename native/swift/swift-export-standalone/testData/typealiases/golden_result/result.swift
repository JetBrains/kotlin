import KotlinBridges
import KotlinRuntime

public typealias RegularInteger = Swift.Int32
public extension main.typealiases.inner {
    public typealias Foo = main.typealiases.Foo
    public typealias LargeInteger = Swift.Int64
    public class Bar : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = typealiases_inner_Bar_init_allocate()
            super.init(__externalRCRef: __kt)
            typealiases_inner_Bar_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}
public extension main.typealiases {
    public typealias Bar = main.typealiases.inner.Bar
    public typealias SmallInteger = Swift.Int16
    public class Foo : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = typealiases_Foo_init_allocate()
            super.init(__externalRCRef: __kt)
            typealiases_Foo_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
}
public enum typealiases {
    public enum inner {
    }
}
