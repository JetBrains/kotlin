// IGNORE_BACKEND: JVM_IR

annotation class Anno

@Target(AnnotationTarget.PROPERTY, AnnotationTarget.CLASS)
annotation class Anno2

class Test {
    @property:[Anno Anno2]
    val prop = "A"
}
