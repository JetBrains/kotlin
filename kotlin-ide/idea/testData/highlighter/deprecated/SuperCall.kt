class MyClass(): Base() {
    fun test2() {
        super.test1()
    }
}

open class Base() {
    fun test1() {}
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS