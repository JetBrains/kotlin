package demo
enum class Color(c : Int) {
private var code : Int = 0
public fun getCode() : Int {
return code
}
{
code = c
}
public fun name() : String { return "" }
public fun order() : Int { return 0 }
}