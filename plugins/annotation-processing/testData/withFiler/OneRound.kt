annotation class Inject
annotation class Inject2

@Inject2
class Test {
    @Inject
    @Inject2
    fun myFunc(): String = "Mary"
}