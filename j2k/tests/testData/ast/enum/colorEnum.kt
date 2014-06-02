package demo

enum class MyEnum private(private val color: Int) {
    RED : MyEnum(10)
    BLUE : MyEnum(20)

    public fun getColor(): Int {
        return color
    }
}