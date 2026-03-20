// WITH_STDLIB

class EverythingC @JvmExposeBoxed("exposed_constructor") constructor(
        val a: Int = 1,
        @IntroducedAt("2") val a1: UInt = 3u,
        @IntroducedAt("3") val b : Boolean = true
   )
   {
    @JvmOverloads
    @JvmName("jvmname_foo")
    @JvmExposeBoxed("exposed_foo")
    fun foo(
        a: Int = 1,
        @IntroducedAt("2") a1: UInt = 3u,
        @IntroducedAt("3") b : Boolean = true,
    ) = "$a/$a1/$b"
}
