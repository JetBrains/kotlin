package test

public class Short(s: String) {
    companion object {
        public fun valueOf(value: String): Short {
            return Short(value)
        }
    }
}

object Test {
    public fun test() {
        test.Short.valueOf("1")
        test.Short.valueOf("1")
        java.lang.Short.valueOf("1")
    }
}