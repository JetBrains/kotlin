namespace demo
enum class MyEnum(_color : Int) {
{
$color = _color
}
RED : MyEnum(10)
BLUE : MyEnum(20)
private val color : Int = 0
public fun getColor() : Int {
return color
}
public fun name() : String { return "" }
public fun order() : Int { return 0 }
}