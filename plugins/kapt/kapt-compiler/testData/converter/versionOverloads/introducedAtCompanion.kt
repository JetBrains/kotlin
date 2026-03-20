class ClassWithCompanion() {
    companion object {
        fun companionFun(
            a: Int,
            @IntroducedAt("1") b: String = "hello",
            @IntroducedAt("2") c: Boolean = true,
        ) = "$this/$b/$c"
    }

    companion {
        fun companionBlockFun(
            a: Int,
            @IntroducedAt("1") b: String = "hello",
            @IntroducedAt("2") c: Boolean = true,
        ) = "$this/$b/$c"
    }

    companion {
        @JvmExposeBoxed
        fun companionBlockFun(
            a: Int,
            @IntroducedAt("1") b: UInt = 3u,
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
