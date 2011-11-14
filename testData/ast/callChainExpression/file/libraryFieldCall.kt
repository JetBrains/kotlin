open class Library(ourOut : PrintStream?) {
class object {
val ourOut : PrintStream?
}
{
$ourOut = ourOut
}
}
open class User() {
open fun main() : Unit {
Library.ourOut?.print()
}
}