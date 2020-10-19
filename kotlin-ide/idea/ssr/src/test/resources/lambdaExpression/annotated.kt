@Target(AnnotationTarget.FUNCTION)
annotation class Ann

fun annotatedLambda() = <warning descr="SSR">@Ann { println() }</warning>
fun notAnnotatedLambda() = { println() }