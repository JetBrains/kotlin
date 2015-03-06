class A {
    private var i: Int? = getByte().toInt()

    fun foo() {
        i = 10
    }

    default object {

        fun getByte(): Byte {
            return 0
        }
    }
}