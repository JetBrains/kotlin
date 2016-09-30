package java.io

@library
public interface Closeable {
    public fun close(): Unit
}

internal interface Serializable