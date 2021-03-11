// PROBLEM: none
enum class SomeEnum {
    <caret>USED
}

fun test(): Array<SomeEnum> {
    return enumValues()
}