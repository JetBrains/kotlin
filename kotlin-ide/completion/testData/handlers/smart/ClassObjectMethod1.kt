package sample

class K {
    companion object {
        fun bar(): K = K()
    }
}

fun foo(){
    val k : K = <caret>
}

// ELEMENT: bar
