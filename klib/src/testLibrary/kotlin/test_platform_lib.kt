package test.konan.platform

fun produceMessage() {
    println("""This is a side effect of a test library linked into the binary.
You should not be seing this.
""")
}

val x: Unit = produceMessage()
