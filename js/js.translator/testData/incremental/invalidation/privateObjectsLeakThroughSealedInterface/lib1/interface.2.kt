sealed interface SealedInterface {
    val extraNumber get() = 1

    fun getNumber() = 0

    fun getOtherNumber() = 1
}
