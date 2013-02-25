enum class Color(c : Int) {
WHITE : Color(21)
BLACK : Color(22)
RED : Color(23)
YELLOW : Color(24)
BLUE : Color(25)
private var code : Int = 0
public fun getCode() : Int {
return code
}
{
code = c
}
}