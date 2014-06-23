package demo

enum class Color private(private val code: Int) {

    public fun getCode(): Int {
        return code
    }
}