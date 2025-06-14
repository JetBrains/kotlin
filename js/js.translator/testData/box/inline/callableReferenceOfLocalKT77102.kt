// ISSUE: KT-77102
// LANGUAGE: +IrInlinerBeforeKlibSerialization
// IGNORE_BACKEND_K2: JS_IR, JS_IR_ES6
// ^^^ KT-77102: IrTypeParameterSymbolImpl is unbound. Signature: null

inline fun <T> inlineGenericTestFunction(f: () -> T) = f()

val updateStatus = inlineGenericTestFunction {
    fun <F> bangbang(flag: F) = flag!!
}

fun box() = "OK"
