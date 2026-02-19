@Target(AnnotationTarget.TYPE)
annotation class A(val values: Array<String> = ["1", "2", "3"])

fun f(): @A Unit {}
