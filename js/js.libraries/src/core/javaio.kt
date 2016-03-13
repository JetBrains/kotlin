package java.io

@library
public class IOException(message: String = "") : Exception() {}

@library
public interface Closeable {
    public open fun close(): Unit
}

interface Serializable
