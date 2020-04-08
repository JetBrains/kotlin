// "Remove setter from property" "true"

class A {
    <caret>lateinit var str: String
        set(value) {}
}