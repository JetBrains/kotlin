abstract open class Shape {
public var color : String?
open public fun setColor(c : String?) : Unit {
color = c
}
open public fun getColor() : String? {
return color
}
abstract open public fun area() : Double
}