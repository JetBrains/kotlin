internal interface I {
    val int: Int
}

internal class C {
    var string: String? = null
    val `object`: Any?
        get() {

            foo(object : I {
                override val int: Int
                    get() = 0
            })
            return string
        }

    fun foo(i: I?) {
    }
}