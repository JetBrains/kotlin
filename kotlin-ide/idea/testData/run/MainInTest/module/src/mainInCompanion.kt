package companions

class A {
    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            // yes
        }
    }
}

class B {
    companion object {
        fun main(args: Array<String>) {
            // no
        }
    }
}

class C {
    companion object {
        @JvmStatic
        @JvmName("main0")
        fun main(args: Array<String>) { // no
        }
    }
}

class D {
    companion object {
        @JvmStatic
        @JvmName("main")
        fun badName(args: Array<String>) { // yes
        }
    }
}
