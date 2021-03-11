// PROBLEM: none
enum class MyEnum(val i: Int) {
    HELLO<caret>(42),
    WORLD("42")
    ;

    constructor(s: String): this(42)
}

fun test() {
    MyEnum.HELLO
}