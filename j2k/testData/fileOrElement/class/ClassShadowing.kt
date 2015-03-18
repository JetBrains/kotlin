package test

public class Short {
    companion object {
        public fun valueOf(value: String): Short {
            return Short()
        }
    }
}

class Test {
    companion object {
        public fun test() {
            test.Short.valueOf("1")
            test.Short.valueOf("1")
            java.lang.Short.valueOf("1")
        }
    }
}