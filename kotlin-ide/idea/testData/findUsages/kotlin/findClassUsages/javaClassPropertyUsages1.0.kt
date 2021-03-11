// PSI_ELEMENT: org.jetbrains.kotlin.psi.KtClass
// OPTIONS: propertyUsages
interface <caret>X {
    val a: String
        get() {
            return ""
        }
    var b: Int
        get () {
            return 0
        }
        set (value: Int) {

        }

    fun foo(s: String) {
        println(s)
    }
}

open class A: X {
    override val a: String
        get() {
            return "?"
        }
    override var b: Int
        get () {
            return 1
        }
        set (value: Int) {
            println(value)
        }

    override fun foo(s: String) {
        println("!$s!")
    }
}
