internal class A {
    private var i: Int? = byte.toInt()

    fun foo() {
        i = 10
    }

    companion object {

        val byte: Byte
            get() = 0
    }
}