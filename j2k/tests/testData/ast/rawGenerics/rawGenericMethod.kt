package demo

open class TestT() {
    open fun <T> getT() {
    }
}

open class U() {
    open fun main() {
        var t: TestT? = TestT()
        t?.getT<String?>()
        t?.getT<Int?>()
        t?.getT()
    }
}