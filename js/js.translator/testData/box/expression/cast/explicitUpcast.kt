// MINIFICATION_THRESHOLD: 515
package foo

open class A

class B : A()

class C

fun box(): String {
    var b: B = createWrongObject()
    if (b is A) return "fail1: is"
    if (b as? A != null) return "fail1: as?"
    try {
        println(b as A)
        return "fail1: as"
    }
    catch (e: ClassCastException) {
        // It's expected
    }

    b = B()
    if (b !is A) return "fail2: is"
    if (b as? A == null) return "fail2: as?"
    try {
        if ((b as A) != b) return "fail2: as"
    }
    catch (e: ClassCastException) {
        return "fail2a: as"
    }

    return "OK"
}

external fun createWrongObject(): B