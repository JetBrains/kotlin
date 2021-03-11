// "Make '<set-attribute>' public" "true"
// ACTION: "Make '<set-attribute>' internal"

class Demo {
    var attribute = "a"
        private set
}

fun main() {
    val c = Demo()
    <caret>c.attribute = "test"
}