package samples

class Standard {
    @Sample
    fun orElse() {
        val nonNullText: String? = "Hello, Kotlin!"

        assertPrints(nonNullText.orElse { "Hello, World!" }, "Hello, Kotlin!")

        val nullText: String? = null

        assertPrints(nullText.orElse { "Hello, World!" }, "Hello, World!")
    }
}
