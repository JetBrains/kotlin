// !DIAGNOSTICS: -UNUSED_VARIABLE

//import kotlin.browser.window
//import kotlinx.cinterop.CPointed
//
fun jvmSpecific(args: Array<String>) {
    val x: Cloneable? = null
    args.clone()
}

//fun nativeSpecific() {
//    val x: CPointed? = null
//}
//
//fun jsSpecific() {
//    val windowClosed = window.closed
//}