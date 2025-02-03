class State @JvmOverloads constructor(
        val someInt: Int,
        val someLong: Long,
        val someString: String = ""
)

class State2 @JvmOverloads constructor(
        @JvmField val someInt: Int,
        @JvmField val someLong: Long = 2,
        @JvmField val someString: String = ""
) {
    @JvmOverloads
    fun test(someInt: Int, someLong: Long = 1, someString: String = "A"): Int = 5

    fun someMethod(str: String) {}
    fun methodWithoutArgs() {}
}
