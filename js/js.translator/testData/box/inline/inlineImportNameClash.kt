// EXPECTED_REACHABLE_NODES: 1286
var l = ""

fun log(message: String) {
    l += message + ";"
}

fun baz(x: String){
    log("baz($x)")
}
fun baz(x: String, i: Int) {
    log("baz($x, $i)")
}

inline fun bar() {
    boo {
        baz("AAA")
        foo()
    }
}

fun boo(x: () -> Unit) = x()

inline fun foo() {
    log("foo()")
    baz("BBB", 333)
}

// CHECK_BREAKS_COUNT: function=box count=0 TARGET_BACKENDS=JS_IR
// CHECK_LABELS_COUNT: function=box name=$l$block count=0 TARGET_BACKENDS=JS_IR
fun box(): String {
    bar()
    if (l != "baz(AAA);foo();baz(BBB, 333);") return "fail: $l"
    return "OK"
}