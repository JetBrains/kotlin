// !IGNORE_FIR

class A(init: Int) {
    private var privateProp = 0 // accesses should be field accesses
    var mutableProp: Int
    init {
        mutableProp = init
    }

    fun add(x: Int): Int {
        val result = privateProp
        privateProp = x
        return privateProp
    }
}

fun properties() {
    val a = A(17)
    val x = -a.mutableProp
    a.mutableProp = 1
    a.mutableProp += x
    ++a.mutableProp
    a.mutableProp--
}

fun A.ext() {
    val x = -mutableProp
    mutableProp = 1
    mutableProp += x
    ++mutableProp
    mutableProp--
}
