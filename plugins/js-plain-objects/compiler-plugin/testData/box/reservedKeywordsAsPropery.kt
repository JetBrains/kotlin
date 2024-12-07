// KT-68891
import kotlinx.js.JsPlainObject

@JsPlainObject
external interface Test {
    val default: String
    val undefined: String
    val await: String
}

fun box(): String {
    val test = Test(default = "O", undefined = "K", await = "")
    return test.default + test.undefined + test.await
}
