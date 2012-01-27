open class Base(name : String?) {
}
open class One(name : String?, second : String?) : Base(name) {
private var mySecond : String? = null
{
mySecond = second
}
}