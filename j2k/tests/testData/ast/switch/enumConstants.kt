enum class ColorEnum {
    GREEN
}

class MyClass {
    fun method(colorEnum: ColorEnum): Int {
        when (colorEnum) {
            ColorEnum.GREEN -> return 1
            else -> return 2
        }
    }
}