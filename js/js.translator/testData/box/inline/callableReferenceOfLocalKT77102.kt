// LANGUAGE: +IrIntraModuleInlinerBeforeKlibSerialization +IrCrossModuleInlinerBeforeKlibSerialization

inline fun <T> inlineGenericTestFunction(f: () -> T) = f()

val updateStatus = inlineGenericTestFunction {
    fun <F> bangbang(flag: F) = flag!!
}

fun box() = "OK"
