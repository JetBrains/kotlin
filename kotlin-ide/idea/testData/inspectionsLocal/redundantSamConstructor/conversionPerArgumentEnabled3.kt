// COMPILER_ARGUMENTS: -XXLanguage:+NewInference -XXLanguage:+SamConversionForKotlinFunctions -XXLanguage:+FunctionalInterfaceConversion -XXLanguage:+SamConversionPerArgument

fun interface KtRunnable {
    fun run()
}

fun test(r1: KtRunnable, r2: KtRunnable) {}

fun usage(r1: KtRunnable) {
    test(r1, KtRunnable<caret> {})
}
