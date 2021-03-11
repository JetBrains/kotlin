// FULL_JDK

class Foo : java.io.Serializable {
    private fun writeObject(s: java.io.ObjectOutputStream) {
        s.toString()
    }

    private fun readObject(s: java.io.ObjectInputStream) {
        s.toString()
    }

    fun writeReplace(): Any = Any()
    fun readResolve(): Any = Any()

    fun readObjectNoData() {
    }
}

fun main(args: Array<String>) {
    Foo()
}