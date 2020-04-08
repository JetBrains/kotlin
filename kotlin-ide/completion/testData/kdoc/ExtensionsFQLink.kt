package a

interface I

class A : I

class B : I {
    /**
     * [a.B.<caret>]
     */
    fun member() {

    }
}

fun B.ext() {
}

val B.extVal: String
    get() = ""

fun A.wrongExt(){}

val A.wrongExtVal: String
    get() = ""

fun I.extForSuper(){}

// EXIST: ext
// EXIST: extVal
// ABSENT: wrongExt
// ABSENT: wrongExtVal
// EXIST: extForSuper
