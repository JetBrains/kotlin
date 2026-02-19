interface AppInterface {
    fun test(f: () -> String) = "foo".run { f() }
}
