class TryCatch {
    fun catches() {
        try {
            body()
        } catch (e: Throwable) {
            catcher()
        } finally {
            finalizer()
        }
    }

    fun body() {}
    fun catcher() {}
    fun finalizer() {}
}

class TryCatchAnnotations {
    @java.lang.SuppressWarnings("Something")
    fun catches() {
        try {
            body()
        } catch (@java.lang.SuppressWarnings("Something") e: Throwable) {
            catcher()
        } finally {
            finalizer()
        }
    }

    fun body() {}
    fun catcher() {}
    fun finalizer() {}
}
