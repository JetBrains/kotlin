abstract class A {
    val x: Int

    abstract val y: Int

    init {
        x = 23
    }

}

class B(override val y: Int): A()

// LINES: 1 1 7 7 2 2 2 12 12 12 12 12 12 12 12
