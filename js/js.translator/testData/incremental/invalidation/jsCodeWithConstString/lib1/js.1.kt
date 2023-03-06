fun test() : dynamic {
    return js("var testObj = { $constKey: 1 }; testObj.$constKey")
}
