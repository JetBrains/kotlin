// "Safe delete 'WORLD'" "true"
enum class MyEnum {
    HELLO,
    WORLD<caret>
}

fun main() {
    MyEnum.HELLO
}