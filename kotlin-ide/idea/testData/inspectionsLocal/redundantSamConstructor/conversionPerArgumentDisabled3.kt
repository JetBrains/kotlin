// COMPILER_ARGUMENTS: -XXLanguage:+NewInference -XXLanguage:+SamConversionForKotlinFunctions -XXLanguage:+FunctionalInterfaceConversion -XXLanguage:-SamConversionPerArgument
// PROBLEM: none

fun test(r1: Runnable, r2: Runnable) {}

fun usage() {
    test(Runnable { return@Runnable }, Runnable<caret> {})
}
