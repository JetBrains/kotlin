package source

fun sourcePackFun(){}

object SourceObject {


    fun other() {
        foo()
    }
}


fun foo() {
    SourceObject.other()
    sourcePackFun()
    bar++
}

var bar = 1

