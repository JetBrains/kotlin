import KotlinBridges

enum namespace {
    enum main {
        public static var foo: Swift.Int32 {
            get {
                return namespace_main_foo_get()
            }
        }
        public static var bar: Swift.Int32 {
            get {
                return namespace_main_bar_get()
            }
            set {
                namespace_main_bar_set(newValue)
            }
        }
        public static func foobar(
            param: Swift.Int32
        ) -> Swift.Int32 {
            return namespace_main_foobar(param)
        }
    }
}

public func baz() -> Swift.Int32 {
    return __root___baz()
}
