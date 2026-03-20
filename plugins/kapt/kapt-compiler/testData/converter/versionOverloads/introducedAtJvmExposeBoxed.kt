// WITH_STDLIB

class EverythingC {
    @JvmOverloads
    @JvmExposeBoxed
    fun foo(
        a: Int = 1,
        @IntroducedAt("2") a1: UInt = 3u,
        @IntroducedAt("3") b : Boolean = true,
    ) = "$a/$a1/$b"

    @JvmOverloads
    @JvmExposeBoxed("exposed_bar")
    fun bar(
        a: Int = 1,
        @IntroducedAt("2") a1: UInt = 3u,
        @IntroducedAt("3") b : Boolean = true,
    ) = "$a/$a1/$b"
}