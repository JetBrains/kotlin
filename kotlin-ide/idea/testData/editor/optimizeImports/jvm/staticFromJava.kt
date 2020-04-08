import Java.field;
import Java.method;

open class Base {
    companion object : Java()
}

class Derived : Base() {
    fun test() {
        val x = field;
        val y = method();
    }
}