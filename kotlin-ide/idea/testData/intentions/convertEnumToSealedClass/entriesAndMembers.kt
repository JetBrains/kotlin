// WITH_RUNTIME

enum class <caret>MyEnum(val s: String = "") {
    FOO("FOO"), BAR("BAR"), DEFAULT();

    fun foo() {

    }
}

fun test(e: MyEnum) {
    if (e == MyEnum.BAR) {
        println()
    }

    val n = when (e) {
        MyEnum.BAR -> 1
        MyEnum.FOO -> 2
        MyEnum.DEFAULT -> 0
    }
}