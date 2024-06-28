sealed interface SealedInterface {
    val extraNumber get() = 2

    fun getNumber() = 0

    fun getOtherNumber() = 1

    val otherExtraNumber get() = 1
}
