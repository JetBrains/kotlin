fun deb<caret>ug(comment: String, body: String.() -> Unit) = body(comment)
fun test() {
    debug("smth") {
        println("top level")
        debug("is print") {
            println(this)
            println(42)
        }
    }
}