fun test() : dynamic {
    return js("var testObj = { $constKey: $constVal }; testObj.$constKey")
}
