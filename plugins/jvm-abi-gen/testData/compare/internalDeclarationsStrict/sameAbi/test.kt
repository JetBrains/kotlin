package test

fun emptyKtClassWorkaround() = Unit
class PublicOuterClass {

    internal interface TransitivelyPublicInterface

    data class Data internal constructor(
        val couldNotBeRemoved: String
    ): TransitivelyPublicInterface
}
