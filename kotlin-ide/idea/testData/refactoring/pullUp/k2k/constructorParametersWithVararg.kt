open class A(n: Int, vararg s: String) {

}

class <caret>B(
        // INFO: {"checked": "true"}
        val i: Int
) : A(1, "2", "3")