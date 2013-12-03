abstract class A() {
    abstract fun callme()

    open fun callmetoo() {
        print("This is a concrete method.")
    }
}