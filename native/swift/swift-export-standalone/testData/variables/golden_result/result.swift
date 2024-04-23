import KotlinBridges

public var baz: Swift.Int32 {
    get {
        return __root___baz_get()
    }
}
public extension main.namespace.main {
    public static var bar: Swift.Int32 {
        get {
            return namespace_main_bar_get()
        }
        set {
            namespace_main_bar_set__TypesOfArguments__int32_t__(newValue)
        }
    }
    public static var foo: Swift.Int32 {
        get {
            return namespace_main_foo_get()
        }
    }
    public static func foobar(
        param: Swift.Int32
    ) -> Swift.Int32 {
        return namespace_main_foobar__TypesOfArguments__int32_t__(param)
    }
}
public enum namespace {
    public enum main {
    }
}
