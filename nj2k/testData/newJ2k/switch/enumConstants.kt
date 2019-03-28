internal enum class ColorEnum {
    GREEN
}

internal class MyClass {
    fun method(colorEnum: ColorEnum?): Int {
        return when (colorEnum) {
            ColorEnum.GREEN -> 1
            else -> 2
        }
    }
}