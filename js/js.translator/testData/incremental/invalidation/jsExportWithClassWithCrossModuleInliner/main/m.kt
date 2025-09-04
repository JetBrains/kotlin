fun box(): String {
    if (foo() != 77) return "Fail foo"
    if (gaz() != 99) return "Fail gaz"
    return "OK"
}
