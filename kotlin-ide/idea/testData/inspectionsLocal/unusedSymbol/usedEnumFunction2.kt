// PROBLEM: none
enum class SomeEnum {
    <caret>USED
}

fun test() {
    SomeEnum.valueOf("USED")
}