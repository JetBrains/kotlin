// "Make constructor parameter a property" "true"

class SomeClass(vararg dismissibleViewTypes: Int) {
    fun someFun() {
        <caret>dismissibleViewTypes
    }
}