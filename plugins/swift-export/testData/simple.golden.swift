enum namespace1 {
    enum main {
        public func foobar() -> Swift.Int32 { fatalError() }
    }
    public func foo() -> Swift.Int32 { fatalError() }
}

enum namespace2 {
    public func bar() -> Swift.Int32 { fatalError() }
}
