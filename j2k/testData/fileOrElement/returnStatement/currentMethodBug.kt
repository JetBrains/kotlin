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

    var string: String? = null

    fun foo(i: I) {}
}