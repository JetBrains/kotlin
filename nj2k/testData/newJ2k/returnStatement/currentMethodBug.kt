internal interface I {
    val int: Int
}

internal class C {
    val `object`: Any?
        get() {
            foo(
                    object : I {
                        override val int: Int
                            get() = 0
                    }
            )
            return string
        }

    fun foo(i: I?) {}
    var string: String? = null
}