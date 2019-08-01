// KJS_WITH_FULL_RUNTIME
// TARGET_BACKEND: JS_IR

@file:JsExport

package foo

//fun foo(k: String): String = "O$k"
//
//val array2d: Array<Array<Int?>> = arrayOf(arrayOf<Int?>(1))
//
//var mutableIntVariable = 20
//
//fun voidReturningFunction() { }
//
//@JsName("RENAMED_USING_JS_NAME")
//fun stringConcatenation(x: String, y: String) = x + y
//
//fun returnNothing(): Nothing = TODO()
//
//val shouldBeNull = null
//
//class C(
//    val zzz: Int = 300
//) {
//
//    val x = 10
//    var y = "20"
//    fun foo(x: Int): String = x.toString()
//
//    data class C1(val name: String) {
//        data class C2(val name: String) {
//            data class C3(val name: String) {
//
//            }
//        }
//    }
//}
//
//data class D(val name: String, val names: Array<String>)
//
//
//fun functionType(x: (Int, String, (Int) -> String) -> Int) {
//}


fun varargFunction(z: String, vararg x: Int, y: Boolean) {
}

fun varargFunction2(z: String, vararg x: String, y: Boolean) {
}

fun <T, S> genericFunction(x: T, y: S): T? = x

fun <T> x(x:T) = x

fun box(): String = "OK"
