package test

class Short(s: String) {
    companion object {
        fun valueOf(value: String): Short {
            return Short(value)
        }
    }
}

internal object Test {
    fun test() {
        test.Short.valueOf("1")
        test.Short.valueOf("1")
        java.lang.Short.valueOf("1")
    }
}