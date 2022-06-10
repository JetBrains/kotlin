package test

//sealed interface Base

class Usage {

    fun doSmth(x: Base) =
        when (x) {
            is A -> "A"
            is B -> "B"
        }
}
