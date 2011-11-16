enum Color(c : Int) {
private var code : Int
open public fun getCode() : Int {
return code
}
{
$code = c
}
}