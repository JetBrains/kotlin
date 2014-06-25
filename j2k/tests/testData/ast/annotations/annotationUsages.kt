import javaApi.*

Anon1(value = array("a"), stringArray = array("b"), intArray = intArray(1, 2), string = "x")
Anon2(value = "a", intValue = 1, charValue = 'a')
Anon3(e = E.A, stringArray = array(), value = *array("a", "b"))
Anon4("x", "y")
Anon5(1)
Anon6(array("x", "y"))
Anon7(javaClass<String>(), javaClass<StringBuilder>())
Anon8(classes = *array(javaClass<String>(), javaClass<StringBuilder>()))
class C {
    Anon5(1) deprecated("") private val field1 = 0

    Anon5(1)
    private val field2 = 0

    Anon5(1) var field3 = 0

    Anon5(1)
    var field4 = 0

    Anon6(array())
    fun foo(deprecated("") p1: Int, deprecated("") Anon5(2) p2: Char) {
        [deprecated("")] [Anon5(3)] val c = 'a'
    }

    Anon5(1) fun bar() {
    }
}
