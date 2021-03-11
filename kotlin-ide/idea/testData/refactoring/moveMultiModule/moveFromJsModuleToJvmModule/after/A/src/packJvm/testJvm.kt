package packJvm

class ClassUsageJvm {
    init {
        listOf("example")
        arrayOf("another")
        Pair(1, "2")
    }

    companion object {
        @JvmStatic fun foo() {

        }
    }
}

class Foo