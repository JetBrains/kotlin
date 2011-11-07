abstract open class Shape {
public var color : String?
public fun setColor(c : String?) : Unit {
color = c
}
public fun getColor() : String? {
return color
}
abstract public fun area() : Double 
}