annotation class MyReceiverAnnotation(val name: String = "")

fun @receiver:MyReceiverAnnotation String.foo() = this.length

val @receiver:MyReceiverAnnotation("RegExp") String.rx : Regex
    get() { return toRegex() }
