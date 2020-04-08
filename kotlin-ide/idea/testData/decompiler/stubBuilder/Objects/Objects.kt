public object Objects {

    val c = 0

    fun f() {
    }

    private object InnerObject : A {
        val c = 0

        fun f() {
        }
    }

    public object OtherObject : NestedClass() {

        val c = 0

        fun f() {
        }
    }

    public open class NestedClass


}

interface A {
}