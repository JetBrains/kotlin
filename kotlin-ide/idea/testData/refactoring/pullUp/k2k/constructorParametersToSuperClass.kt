open class A(n: Int) {

}

class <caret>B(
        // INFO: {"checked": "true"}
        val s: String,
        // INFO: {"checked": "true"}
        val b: Boolean,
        val i: Int
) : A(1)