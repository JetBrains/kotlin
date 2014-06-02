package demo

enum class Color private(private var code: Int) {

    public fun getCode(): Int {
        return code
    }
}