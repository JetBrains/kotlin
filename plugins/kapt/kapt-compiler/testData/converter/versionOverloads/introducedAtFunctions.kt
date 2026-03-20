fun topLevelFun(
    a: Int,
    @IntroducedAt("1") b: String = "hello",
    @IntroducedAt("2") c: Boolean = true,
) {}


fun Int.extendFun(
    a: Int,
    @IntroducedAt("1") b: String = "hello",
    @IntroducedAt("2") c: Boolean = true,
) {}

suspend fun suspendFun(
    a : Int = 1,
    @IntroducedAt("1") b: String = "hello",
    @IntroducedAt("2") c: Boolean = true,
) {}

@JvmName("javaName")
fun kotlinName(
    a : Int = 1,
    @IntroducedAt("1") b: String = "hello",
    @IntroducedAt("2") c: Boolean = true,
) {}

context(d: Boolean) fun contextFun(
    a : Int = 1,
    @IntroducedAt("1") b: String = "hello",
    @IntroducedAt("2") c: Boolean = true,
) {}

fun inTrailing(
    x: String,
    @IntroducedAt("1") y: Int = 1,
    @IntroducedAt("2") z: Boolean = true,
    block: (String) -> String = { x.uppercase() }
) {}