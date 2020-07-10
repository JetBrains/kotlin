@Target(AnnotationTarget.FUNCTION)
annotation class Ann

fun annotatedLambda() = <warning descr="SSR">@Ann <warning descr="SSR">{ println() }</warning></warning>
fun notAnnotatedLambda() = <warning descr="SSR">{ println() }</warning>