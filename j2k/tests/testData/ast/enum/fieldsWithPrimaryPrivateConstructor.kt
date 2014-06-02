enum class Color private(private var code: Int) {
    WHITE : Color(21)
    BLACK : Color(22)
    RED : Color(23)
    YELLOW : Color(24)
    BLUE : Color(25)

    public fun getCode(): Int {
        return code
    }
}