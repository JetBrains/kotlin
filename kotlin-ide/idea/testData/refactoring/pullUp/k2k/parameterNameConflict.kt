open class A(x: Int) {

}

class <caret>B(
        // INFO: {"checked": "true"}
        val x: String
) : A(1)