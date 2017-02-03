open class M2 {
    inline fun foo() = 1
    inline fun <reified T : Any> bar() = T::class
}