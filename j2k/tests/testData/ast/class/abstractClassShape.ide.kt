abstract class Shape() {
public var color : String = 0
public open fun setColor(c : String) : Unit {
color = c
}
public open fun getColor() : String {
return color
}
public abstract fun area() : Double
}