import KotlinBridges_main
import KotlinRuntime

public typealias Clazz = main.org.kotlin.foo.Clazz
public typealias Typealias = main.org.kotlin.foo.Typealias
public typealias bar = main.org.kotlin.foo.bar
public var constant: Swift.Int32 {
    get {
        main.org.kotlin.foo.constant
    }
}
public var variable: Swift.Int32 {
    get {
        main.org.kotlin.foo.variable
    }
    set {
        main.org.kotlin.foo.variable = newValue
    }
}
public func function(
    arg: Swift.Int32
) -> Swift.Int32 {
    main.org.kotlin.foo.function(arg: arg)
}
public extension main.org.kotlin.foo.bar {
    public typealias Integer = Swift.Int32
}
public extension main.org.kotlin.baz {
    public typealias Integer = Swift.Int32
}
public extension main.org.kotlin.foo {
    public typealias Typealias = Swift.Int32
    public class Clazz : KotlinRuntime.KotlinBase {
        public override init() {
            let __kt = org_kotlin_foo_Clazz_init_allocate()
            super.init(__externalRCRef: __kt)
            org_kotlin_foo_Clazz_init_initialize__TypesOfArguments__uintptr_t__(__kt)
        }
        public override init(
            __externalRCRef: Swift.UInt
        ) {
            super.init(__externalRCRef: __externalRCRef)
        }
    }
    public static var constant: Swift.Int32 {
        get {
            return org_kotlin_foo_constant_get()
        }
    }
    public static var variable: Swift.Int32 {
        get {
            return org_kotlin_foo_variable_get()
        }
        set {
            return org_kotlin_foo_variable_set__TypesOfArguments__int32_t__(newValue)
        }
    }
    public static func function(
        arg: Swift.Int32
    ) -> Swift.Int32 {
        return org_kotlin_foo_function__TypesOfArguments__int32_t__(arg)
    }
}
public enum org {
    public enum kotlin {
        public enum baz {
        }
        public enum foo {
            public enum bar {
            }
        }
    }
}
