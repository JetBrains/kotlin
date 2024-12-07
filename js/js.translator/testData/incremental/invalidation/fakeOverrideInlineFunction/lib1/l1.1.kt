abstract class ClassA {
    inline fun <reified T> Any.castTo(): T? = (this as? T) ?: "OTHER ${this as? Int}" as T
    abstract fun test1(): String?
    abstract fun test2(): String?

    inline fun fakeOverrideFunction() = 0
}
