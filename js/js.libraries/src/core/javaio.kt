package java.io

@library
public class IOException(message: String = "") : Exception() {}

@library
public interface Closeable {
    public fun close(): Unit
}

interface Serializable
