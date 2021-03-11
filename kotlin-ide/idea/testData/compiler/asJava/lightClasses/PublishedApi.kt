// A
class A {
    inline fun test() {
        {
            `access$test`(1)
        }()
    }

    @PublishedApi
    internal fun `access$test`(p: Int) = p
}
