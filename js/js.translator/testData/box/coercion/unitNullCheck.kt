// IGNORE_BACKEND: JS_IR
// EXPECTED_REACHABLE_NODES: 1282
var log = ""

fun log(msg: String) {
    log += "$msg;"
}

fun box(): String {
    log("!!")!!

    log("elvis-left") ?: log("elvis-right")

    if (log("if") == null) log("then")

    when (null) {
        log("when-pattern") -> log("when-body-1")
    }

    when (log("when-subject")) {
        null -> log("when-body-2")
    }

    if (log != "!!;elvis-left;if;when-pattern;when-subject;") return "fail: $log"

    return "OK"
}
