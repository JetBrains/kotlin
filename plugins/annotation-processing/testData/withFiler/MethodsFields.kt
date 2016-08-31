annotation class Inject

class Test {
    @Inject
    fun myFunc(): String = "Mary"

    @field:Inject
    val myField: String = "Tom"
}