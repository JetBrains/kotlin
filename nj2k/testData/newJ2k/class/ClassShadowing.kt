package test

class Integer(s: String) {
    companion object {
        fun valueOf(value: String): Integer {
            return Integer(value)
        }
    }
}

internal object Test {
    fun test() {
        Integer.valueOf("1")
        Integer.valueOf("1")
        java.lang.Integer.valueOf("1")
    }
}