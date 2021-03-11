package dependency

@Target(AnnotationTarget.FIELD)
annotation class A(val s: String)

@Target(AnnotationTarget.FIELD)
annotation class B(val i: Int)
