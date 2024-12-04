fun test() : dynamic {
    return js("var testObj = { ${constKey1}__bar: $constVal + 1 }; testObj.foo__$constKey2")
}
