internal enum class ColorEnum {
    GREEN
}

internal class MyClass {
    fun method(colorEnum: ColorEnum): Int {
        when (colorEnum) {
            ColorEnum.GREEN -> return 1
            else -> return 2
        }
    }
}