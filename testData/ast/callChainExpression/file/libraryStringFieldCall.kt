open class Library(myString : String?) {
public val myString : String?
{
this.myString = myString
}
}
open class User() {
open fun main() : Unit {
Library.myString?.isEmpty()
}
}