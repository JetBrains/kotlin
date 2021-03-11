fun outer() {
    fun local() {

    }

    @Suppress("unused")
    fun localNoWarn() {

    }
}

@Suppress("unused")
fun otherFun() {
    fun localNoWarn() {

    }
}