
// WITH_STDLIB
@file:OptIn(ExperimentalVersionOverloading::class)

fun topLevelFun(
    a: Int,
    @IntroducedAt("1") b: String = "hello",
    @IntroducedAt("2") c: Boolean = true,
) = "$this/$b/$c"
