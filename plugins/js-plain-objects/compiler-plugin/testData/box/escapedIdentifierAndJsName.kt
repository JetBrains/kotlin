// KT-72474
import kotlinx.js.JsPlainObject

@JsPlainObject
external interface Base {
    val `first escaped`: String

    @JsName("second escaped")
    val value: String
}

@JsPlainObject
external interface Test : Base {
    val `own escaped`: String

    @JsName("own @JsName")
    val test: String

    // To test that we collect overridden symbols @JsName
    override val value: String
}

fun box(): String {
    val base = Base(`first escaped` = "O", value = "K")

    if (base.`first escaped` != "O") return "Fail: direct access to `first escaped` gave '${base.`first escaped`}'"
    if (base.value != "K") return "Fail: direct access to value gave '${base.value}'"

    if (base.asDynamic()["first escaped"] != "O") return "Fail: dynamic direct access to `first escaped` gave '${base.asDynamic()["first escaped"]}'"
    if (base.asDynamic()["second escaped"] != "K") return "Fail: dynamic direct access to value gave '${base.asDynamic()["second escaped"]}'"

    val test = Test(`first escaped` = "O", value = "K", `own escaped` = "E", test = "Y")

    if (test.`first escaped` != "O") return "Fail: indirect access to `first escaped` gave '${test.`first escaped`}'"
    if (test.value != "K") return "Fail: indirect access to value gave '${test.value}'"
    if (test.`own escaped` != "E") return "Fail: direct access to `own escaped` gave '${test.`own escaped`}'"
    if (test.test != "Y") return "Fail: direct access to test gave '${test.test}'"

    if (test.asDynamic()["first escaped"] != "O") return "Fail: dynamic indirect access to `first escaped` gave '${test.asDynamic()["first escaped"]}'"
    if (test.asDynamic()["second escaped"] != "K") return "Fail: dynamic indirect access to value gave '${test.asDynamic()["second escaped"]}'"
    if (test.asDynamic()["own escaped"] != "E") return "Fail: dynamic direct access to `own escaped` gave '${test.asDynamic()["own escaped"]}'"
    if (test.asDynamic()["own @JsName"] != "Y") return "Fail: dynamic direct access to test gave '${test.asDynamic()["own @JsName"]}'"

    if (test.asDynamic()["first escaped"] != "O") return "Fail: dynamic direct access to `first escaped` gave '${base.asDynamic()["first escaped"]}'"
    if (test.asDynamic()["second escaped"] != "K") return "Fail: dynamic direct access to value gave '${base.asDynamic()["second escaped"]}'"

    return "OK"
}
