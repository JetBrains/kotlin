// "Add annotation target" "true"
class Foo

@Target
annotation class ReceiverAnn

fun <caret>@receiver:ReceiverAnn Foo.test() {}
