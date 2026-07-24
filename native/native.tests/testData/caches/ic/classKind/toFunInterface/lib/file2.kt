package test

fun run(): String {
    val p = object : Printer {
        override fun render(): String = "printed"
    }
    return p.render()
}
