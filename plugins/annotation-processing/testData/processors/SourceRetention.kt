@Retention(AnnotationRetention.SOURCE)
annotation class Source1

@Retention(AnnotationRetention.SOURCE)
annotation class Source2

@Retention(AnnotationRetention.SOURCE)
annotation class Source3

@Retention(AnnotationRetention.SOURCE)
annotation class Source4

@Retention(AnnotationRetention.BINARY)
annotation class Binary

@Retention(AnnotationRetention.RUNTIME)
annotation class Runtime

@Source1
class Test

class Test2 {
    @Source2
    fun t() {}
}

class Test3 {
    @field:Source3
    val p: String = "A"
}

class Test4 {
    fun t(@Source4 a: String) {}
}

class Test5 {
    @Retention(AnnotationRetention.SOURCE)
    annotation class Source5

    @Source5
    fun t() {}
}