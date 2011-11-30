enum class Color(c : Int) {
{
$code = c
}
private var code : Int = 0
open public fun getCode() : Int {
return code
}
public fun name() : String { return "" }
public fun order() : Int { return 0 }
}