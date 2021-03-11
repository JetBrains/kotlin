// MODE: property

enum class E {
    ENTRY;
    companion object {}
}
val test<# : E# > = E.Companion
