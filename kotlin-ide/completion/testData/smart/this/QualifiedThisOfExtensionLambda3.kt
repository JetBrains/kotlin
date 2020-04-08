fun<T> accept1(handler: String.(t: T) -> Unit){}
fun accept2(handler: Int.() -> Unit){}

fun foo(){
    accept1<String>({
                        accept2({
                                    val s: String = <caret>
                                })
                    })
}

// EXIST: this@accept1
