fun main() {
    value.ok += "!A!"
}

fun returnFalse() = false

fun callMainFromSecond() {
    if (returnFalse()) {
        main()
    }
}
