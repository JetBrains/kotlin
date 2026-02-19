public protocol Foo: KotlinRuntime.KotlinBase, KotlinRuntime.Fooable, KotlinRuntime.Barable {
    var bar: Swift.Bool {
        get
        set
    }
    func foo() -> Swift.Bool
    init()
}