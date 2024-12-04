package foo.bar

var ok = "fail"

fun main(args: Array<String>) {
    if (args.size != 0) {
        ok = "Fail with args zie"
    } else {
        ok = "OK"
    }
}