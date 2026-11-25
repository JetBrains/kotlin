@_cdecl("top_level_functions_foo")
package func top_level_functions_foo() -> Swift.Bool {
    let _result: Swift.Void = top_level_functions.foo()
    return { _result; return true }()
}

@_cdecl("top_level_functions_bar")
package func top_level_functions_bar() -> Swift.Bool {
    let _result: Swift.Void = top_level_functions.bar()
    return { _result; return true }()
}
