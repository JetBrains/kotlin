object X {
    fun accept1(handler: String.() -> Unit){}
    fun accept2(handler: Int.() -> Unit){}
}

fun foo(){
    X.accept1({
                X.accept2({
                            val s: String = <caret>
                        })
            })
}

// EXIST: this@accept1
