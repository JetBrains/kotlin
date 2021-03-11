package target

import source.SourceObject
import source.sourcePackFun

fun targetPackFun(){}


fun foo() {
    SourceObject.other()
    sourcePackFun()
    targetPackFun()
    bar++
}

var bar = 1
