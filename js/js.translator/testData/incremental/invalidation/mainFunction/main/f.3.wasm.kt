package foo.bar

var ok: String = "fail"

var callback: () -> Unit = {}

suspend fun main(args: Array<String>) { //Wasm does not support unintercepted coroutines call
    if (0 != args.size) error("Fail")
    callback = {
        ok = "OK"
    }
}