@Target(AnnotationTarget.FUNCTION)
annotation class Ann

fun annotatedLambda() = @Ann { println() }
<warning descr="SSR">fun notAnnotatedLambda() = { println() }</warning>