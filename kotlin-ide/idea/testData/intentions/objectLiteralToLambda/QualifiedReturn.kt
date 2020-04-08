// WITH_RUNTIME

fun foo(runnable: Runnable) {}

fun bar(list: List<String>) {
    foo(<caret>object : Runnable {
        override fun run() {
            list.filter(fun (element: String): Boolean {
                if (element == "a") return false
                if (element == "b") return@run
                return true
            })
        }
    })
}
