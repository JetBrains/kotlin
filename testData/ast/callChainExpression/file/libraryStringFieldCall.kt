open class Library(myString : String?) {
public val myString : String?
{
$myString = myString
}
}
open class User() {
open fun main() : Unit {
Library.myString?.isEmpty()
}
}