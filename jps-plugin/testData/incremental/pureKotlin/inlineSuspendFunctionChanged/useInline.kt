package inline

suspend fun useF() {
    f { println("useF") }
}
