package funinterfaces

fun interface FunInterface {
    fun run(): Int
}

fun getObject(): FunInterface {
    return object : FunInterface {
        override fun run() = 1
    }
}

fun getLambda(): FunInterface {
    return FunInterface { 2 }
}
