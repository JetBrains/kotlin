class Other {
    fun foo() {}
}

actual typealias Some = Other

fun createSome(): Some = Other()
