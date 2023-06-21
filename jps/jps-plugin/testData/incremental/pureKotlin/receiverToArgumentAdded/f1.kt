interface InterfaceA {
    fun test(f: () -> String) = "foo".run { f() }
}
