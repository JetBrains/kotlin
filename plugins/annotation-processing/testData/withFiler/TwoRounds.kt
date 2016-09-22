annotation class Inject
annotation class Inject2

@Inject
class Test {
    @Inject
    fun myFunc(): String = "Mary"
}