package test

public interface LoadIterator<T> {
    public fun getIterator(): MutableIterator<T>?
    public fun setIterator(p0: Iterator<T>?)
}
