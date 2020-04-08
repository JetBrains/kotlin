class Z<T> {
    fun f(): Z<T> = ext<caret>
}

private fun <T> Z<T>.extFun() = Z<T>()

// EXIST: "extFun"
