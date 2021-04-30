// !IGNORE_FIR

interface Callback {
    fun onError(throwable: Throwable)
}

class Model {
    init {
        crashMe(Callback::class.java) {
            object : Callback {
                override fun onError(throwable: Throwable) {
                    throw UnsupportedOperationException("")
                }
            }
        }
    }

    fun <T : Any> crashMe(clazz: Class<T>, factory: () -> T) {
        throw UnsupportedOperationException()
    }
}
