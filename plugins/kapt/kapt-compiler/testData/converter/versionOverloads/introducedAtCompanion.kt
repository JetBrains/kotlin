class ClassWithCompanion() {
    companion object {
        fun companionFun(
            a: Int,
            @IntroducedAt("1") b: String = "hello",
            @IntroducedAt("2") c: Boolean = true,
        ) = "$this/$b/$c"
    }
}

class ClassWithJvmStaticCompanion() {
    companion object {
        @JvmStatic
        fun staticCompanionFun(
            a: Int,
            @IntroducedAt("1") b: String = "hello",
            @IntroducedAt("2") c: Boolean = true,
        ) = "$this/$b/$c"
    }
}