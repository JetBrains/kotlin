package testing.rename

interface A : Runnable {
    override fun run() {
        TODO("Not yet implemented")
    }
}

public open class B: A {
    override fun run() {

    }
}