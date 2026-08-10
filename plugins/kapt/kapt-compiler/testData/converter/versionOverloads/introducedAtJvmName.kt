// WITH_STDLIB

class EverythingC {
    @JvmName("jvmname_foo")
    fun foo(
        a: Int = 1,
        @IntroducedAt("2") a1: String = "a1",
        @IntroducedAt("3") b : Boolean = true,
    ) = "$a/$a1/$b"
}
