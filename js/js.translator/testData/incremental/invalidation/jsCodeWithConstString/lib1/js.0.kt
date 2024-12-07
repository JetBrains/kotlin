fun test() : dynamic {
    return js("var testObj = { $constKey: 0 }; testObj.$constKey")
}
