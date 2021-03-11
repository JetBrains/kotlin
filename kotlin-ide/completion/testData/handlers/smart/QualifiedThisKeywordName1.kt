class `this` {
    fun String.foo(){
        val foo: `this` = <caret>
    }
}

// ELEMENT: "this@`this`"

