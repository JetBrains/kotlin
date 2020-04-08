package source

import target.foo

fun sourcePackFun(){}

object SourceObject {


    fun other() {
        foo()
    }
}
