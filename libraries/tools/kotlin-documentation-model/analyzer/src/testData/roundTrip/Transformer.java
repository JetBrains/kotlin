// MODULE: main
// FILE: example/Transformer.java
package example;

public interface Transformer<T> {
    <R> R convert(T value, R fallback);

    T transform(T value);
}
