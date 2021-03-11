fun returnDynamic(): dynamic = TODO()
fun returnString(): String = ""
fun returnNothing(): Nothing? = TODO()

val test: Any? = return<caret>

// WITH_ORDER
// EXIST: returnString
// EXIST: returnDynamic
// EXIST: returnNothing
