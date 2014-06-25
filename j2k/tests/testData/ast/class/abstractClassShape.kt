abstract class Shape {
    public var color: String = 0
    public fun setColor(c: String) {
        color = c
    }
    public fun getColor(): String {
        return color
    }
    public abstract fun area(): Double
}