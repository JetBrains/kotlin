import java.io.Serializable

class Bar : Serializable {
    var foobar = 0

    companion object {
        private const val serialVersionUID: Long = 0
    }
}

class Foo {
    var foobar = 0

    companion object {
        private const val serialVersionUID: Long = 0
    }
}