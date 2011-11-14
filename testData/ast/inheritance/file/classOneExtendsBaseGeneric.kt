open class Base<T>(name : T?) {
}
open class One<T, K>(name : T?, second : K?) : Base<T?>(name) {
private var mySecond : K?
{
$mySecond = second
}
}