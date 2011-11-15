open class Base(myFirst : String?) {
private var myFirst : String?
{
$myFirst = myFirst
}
}
open class Child(mySecond : String?) : Base() {
private var mySecond : String?
{
$mySecond = mySecond
}
}