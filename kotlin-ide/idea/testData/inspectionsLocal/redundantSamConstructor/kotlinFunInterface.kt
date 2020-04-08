// COMPILER_ARGUMENTS: -XXLanguage:+NewInference -XXLanguage:+SamConversionForKotlinFunctions -XXLanguage:+FunctionalInterfaceConversion

fun interface KtRunnable {
    fun run()
}

class Test {
    fun usage(r: KtRunnable) {}

    fun test() {
        usage(KtRunnable<caret> { })
    }
}