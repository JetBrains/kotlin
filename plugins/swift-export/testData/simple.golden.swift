enum namespace1 {
    enum main {
        public func foobar(
            param: Swift.Int32
        ) -> Swift.Int32 {
            return namespace1_main_foobar(param)
        }
    }
    public func foo() -> Swift.Int32 {
        return namespace1_foo()
    }
}

enum namespace2 {
    public func bar() -> Swift.Int32 {
        return namespace2_bar()
    }
}
