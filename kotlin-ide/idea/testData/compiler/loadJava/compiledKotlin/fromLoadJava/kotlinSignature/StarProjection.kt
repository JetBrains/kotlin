package test

public final class StarProjection {
    public final fun foo(): MyClass<*> = throw UnsupportedOperationException()

    public interface MyClass<T: CharSequence?>
}
