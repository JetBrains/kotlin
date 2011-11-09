enum Color {
WHITE(21)
BLACK(22)
RED(23)
YELLOW(24)
BLUE(25)
private var code : Int
open private (c : Int) {
$code = c
}
open public fun getCode() : Int {
return code
}
}