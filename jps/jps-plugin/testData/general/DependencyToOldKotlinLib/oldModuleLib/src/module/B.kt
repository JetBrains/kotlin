package module

public class B(private val c: C) {
    fun foo() {
        val a = c.getA()
        a.oldFun()
    }
}
