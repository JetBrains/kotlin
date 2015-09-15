internal class A {
    private var i: Int? = getByte().toInt()

    internal fun foo() {
        i = 10
    }

    companion object {

        internal fun getByte(): Byte {
            return 0
        }
    }
}