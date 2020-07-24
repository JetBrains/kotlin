class A(var constructorVar: Int) {
    private var privateProp = 0 // accesses should be field accesses
    var mutableProp: Int
    init {
        mutableProp = constructorVar
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
    a.constructorVar += x
    ++a.constructorVar
    a.mutableProp--
}

fun A.ext() {
    val x = -constructorVar
    mutableProp = 1
    constructorVar += x
    ++constructorVar
    mutableProp--
}
