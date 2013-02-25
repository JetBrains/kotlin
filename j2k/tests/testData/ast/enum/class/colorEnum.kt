package demo
enum class MyEnum(_color : Int) {
RED : MyEnum(10)
BLUE : MyEnum(20)
private val color : Int
public fun getColor() : Int {
return color
}
{
color = _color
}
}