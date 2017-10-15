class SimpleAnnotated {
    @Suppress("abc")
    fun method() {
        println("Hello, world!")
    }

    @SinceKotlin("1.0")
    val property: String = "Mary"
}