typealias <caret>R1 = Runnable
val r1 = R1 {} // (1)
val r2 = object : R1 { // (2)
    override fun run() {}
}