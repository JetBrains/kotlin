// EXPECTED_REACHABLE_NODES: 1281
// ES_MODULES
// CALL_MAIN

var o: String = "fail O"
var k: String = "K"

fun main() {
    k = "fail K"
}

fun main(args: Array<String>) {
    if (args.size != 0) error("Fail")

    o = "O"
}

fun box() = o + k