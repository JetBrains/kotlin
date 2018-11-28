// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1284
var log = ""

fun foo() {
    log += "foo"
}

fun Unit.bar() = jsTypeOf(this.asDynamic()) == "undefined"

fun Any.baz() = jsTypeOf(this.asDynamic()) == "object"

fun box(): String {
    if (!foo().bar()) return "fail1"
    if (!foo().baz()) return "fail2"

    return "OK"
}