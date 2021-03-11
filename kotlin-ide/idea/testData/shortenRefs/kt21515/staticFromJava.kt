open class Base {
    companion object : Java()
}

class Derived : Base() {
    fun test() {
        val x = <selection>Java.field</selection>;
        val y = <selection>Java.method()</selection>;
    }
}