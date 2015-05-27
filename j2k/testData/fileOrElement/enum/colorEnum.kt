package demo

enum class MyEnum private constructor(public val color: Int) {
    RED : MyEnum(10)
    BLUE : MyEnum(20)
}