internal class A {
    private var i = byte.toInt()

    fun foo() {
        i = 10
    }

    companion object {

        val byte: Byte
            get() = 0
    }
}