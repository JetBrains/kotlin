annotation class Anno

@Anno
class A {
    interface AA {
        fun onClick(o: Any)
    }
}

enum class B { RED }

interface C {
    fun onClick()
}

interface D {
    fun onClick()
    fun onDoubleClick()
}

interface E : C

interface F : C {
    fun onDoubleClick()
}