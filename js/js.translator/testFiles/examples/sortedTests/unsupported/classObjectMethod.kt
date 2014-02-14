abstract open class Default {
    abstract fun defaultValue(): Int
}

class MyInt() {
    class object : Default {
        override fun defaultValue(): Int = 610
    }
}

fun toDefault<T : Any>(t: T) where class object T : Default = T.defaultValue()

fun box(): String = if (toDefault<MyInt>(MyInt()) == 610) "OK" else "fail"
