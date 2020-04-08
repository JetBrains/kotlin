// "Change to var" "true"

class Test {
    val a: String

    init {
        val t = object {
            fun some() {
                <caret>a = "12"
            }
        }

        a = "2"
        t.some()
    }
}