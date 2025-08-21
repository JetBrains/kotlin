@Target(AnnotationTarget.TYPE)
annotation class A


fun foo(callback: @A (parameter: (() -> Unit)) -> Unit) { }