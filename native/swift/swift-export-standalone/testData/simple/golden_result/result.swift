import KotlinBridges

enum namespace1 {
    enum main {
        public static func foobar(
            param: Swift.Int32
        ) -> Swift.Int32 {
            return namespace1_main_foobar(param)
        }
    }
    public static func bar() -> Swift.Int32 {
        return namespace1_bar()
    }
}

enum namespace2 {
    public static func foo() -> Swift.Int32 {
        return namespace2_foo()
    }
}

public func foo() -> Swift.Int32 {
    return __root___foo()
}
