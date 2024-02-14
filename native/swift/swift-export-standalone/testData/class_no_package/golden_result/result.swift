import KotlinBridges

public enum Foo {
}

public func bar(
    p: Foo
) -> Foo {
    fatalError("YET UNSUPPORTED")
}

public var foo: Foo {
    get {
        fatalError("YET UNSUPPORTED")
    }
    set {
        fatalError("YET UNSUPPORTED")
    }
}
