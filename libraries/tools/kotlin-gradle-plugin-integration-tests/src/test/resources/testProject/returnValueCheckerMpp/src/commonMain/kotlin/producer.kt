@MustUseReturnValues
class Producer {
    fun produce(): String = "result"
}

fun ignoreInCommon() {
    // ignoring the result of a must-use declaration is reported by the return value checker;
    // common sources are compiled together with each platform, so the warning appears in every platform compilation
    Producer().produce()
}
