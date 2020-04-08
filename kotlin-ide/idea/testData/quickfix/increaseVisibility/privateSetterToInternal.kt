// "Make '<set-attribute>' internal" "true"
// ACTION: "Make '<set-attribute>' public"

class Demo {
    var attribute = "a"
        private set
}

fun main() {
    val c = Demo()
    <caret>c.attribute = "test"
}