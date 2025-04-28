// LANGUAGE: +IrInlinerBeforeKlibSerialization
// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// ^^^ KT-77102: Function 'CHECK_NOT_NULL' can not be called: Expression uses unlinked type parameter symbol '[ /updateStatus|{}updateStatus[0] <- Local[<BF>] ]:3:4:5' declared in file callableReferenceOfLocalKT77102.kt

inline fun <T> inlineGenericTestFunction(f: () -> T) = f()

val updateStatus = inlineGenericTestFunction {
    fun <F> bangbang(flag: F) = flag!!
}

fun box() = "OK"
