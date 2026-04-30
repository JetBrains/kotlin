package java.io

public open class StringWriter {
    override fun toString(): String = TODO()
}

public open class PrintWriter(out: Any) {
    public open fun flush() {}
}

public open class IOException : Exception()

