class A {
    private var i: Int? = getByte().toInt()

    fun foo() {
        i = 10
    }

    companion object {

        fun getByte(): Byte {
            return 0
        }
    }
}