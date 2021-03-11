package source

import target.targetPackFun

fun sourcePackFun(){}

object SourceObject {
    <selection>
    fun foo() {
        other()
        sourcePackFun()
        targetPackFun()
        bar++
    }

    var bar = 1
    </selection>

    fun other() {
        foo()
    }
}
