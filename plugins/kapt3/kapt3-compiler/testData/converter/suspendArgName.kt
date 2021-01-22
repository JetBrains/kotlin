open class Test {

    open fun getTestNoSuspend(text: String): String {
        return text
    }

    open suspend fun getTest(text: String): String {
        return text
    }
}