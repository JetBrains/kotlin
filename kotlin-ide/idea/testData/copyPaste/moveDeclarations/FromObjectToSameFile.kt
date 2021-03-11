package source

fun sourcePackFun(){}

object SourceObject {
    <selection>
    fun foo() {
        other()
        sourcePackFun()
        bar++
    }

    var bar = 1
    </selection>

    fun other() {
        foo()
    }
}

<caret>
