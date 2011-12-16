enum class Color(c : Int) {
{
code = c
}
WHITE : Color(21)
BLACK : Color(22)
RED : Color(23)
YELLOW : Color(24)
BLUE : Color(25)
private var code : Int = 0
public fun getCode() : Int {
return code
}
public fun name() : String { return "" }
public fun order() : Int { return 0 }
}