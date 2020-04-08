fun `fun`(handler: String.() -> Unit){}
fun bar(handler: Int.() -> Unit){}

fun foo(){
    `fun`({
                bar({
                            val s: String = <caret>
                        })
            })
}

// ELEMENT: this@`fun`
