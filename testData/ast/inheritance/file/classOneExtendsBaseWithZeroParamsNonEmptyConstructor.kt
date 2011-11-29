open class Base(name : String?) {
}
open class One(name : String?, second : String?) : Base(name) {
{
$mySecond = second
}
private var mySecond : String? = null
}