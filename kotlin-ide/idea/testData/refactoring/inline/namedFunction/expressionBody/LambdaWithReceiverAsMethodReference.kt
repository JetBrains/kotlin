fun de<caret>bug(comment: String, body: String.() -> Unit) = body(comment)
fun test() {
    debug("text", ::println)
}