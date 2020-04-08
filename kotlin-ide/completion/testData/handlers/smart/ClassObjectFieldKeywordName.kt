package `package`

class `class` {
    companion object {
        val `val` = `class`()
    }
}

fun foo(){
    val v: `class`= <caret>
}

// ELEMENT: val
