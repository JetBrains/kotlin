package java.io

library
public class IOException(message: String = "") : Exception() {}

library
public trait Closeable {
    public open fun close() : Unit;
}
