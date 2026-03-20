class EverythingC {
    @JvmOverloads
    fun foo_no_introduce(
        a: Int = 1,
        a1: String = "hello",
        b : Boolean = true,
    ) = "$a/$a1/$b"

    @JvmOverloads
    fun foo_with_introduce(
        a: Int = 1,
        @IntroducedAt("2") a1: String = "hello",
        @IntroducedAt("3") b : Boolean = true,
    ) = "$a/$a1/$b"
}