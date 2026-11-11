@_cdecl("classes_Foo_init")
package func classes_Foo_init() -> Any {
    let _result: classes.Foo = classes.Foo()
    return _result
}

@_cdecl("classes_Foo_bar")
package func classes_Foo_bar(_ `self`: Any) -> Swift.Bool {
    let _self = `self` as! classes.Foo
    let _result: Swift.Void = _self.bar()
    return { _result; return true }()
}

@_cdecl("classes_Foo_baz")
package func classes_Foo_baz(_ `self`: Any) -> Swift.Bool {
    let _self = `self` as! classes.Foo
    let _result: Swift.Void = _self.baz()
    return { _result; return true }()
}

@_cdecl("classes_PureSwift_init")
package func classes_PureSwift_init() -> Any {
    let _result: classes.PureSwift = classes.PureSwift()
    return _result
}

@_cdecl("classes_PureSwift_foo")
package func classes_PureSwift_foo(_ `self`: Any) -> Swift.Bool {
    let _self = `self` as! classes.PureSwift
    let _result: Swift.Void = _self.foo()
    return { _result; return true }()
}
