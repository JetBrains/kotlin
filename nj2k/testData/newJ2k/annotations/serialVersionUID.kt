import java.io.Serializable

class Bar : Serializable {
    internal var foobar = 0

    companion object {
        private const val serialVersionUID: Long = 0
    }
}

class Foo {
    internal var foobar = 0

    companion object {
        private const val serialVersionUID: Long = 0
    }
}