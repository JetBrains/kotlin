annotation class MyTestAnnotation

fun unused(<warning descr="[UNUSED_PARAMETER] Parameter 'p' is never used">p</warning>: Int) {

}

@MyTestAnnotation
fun unusedButAnnotated(p: Int) {

}

