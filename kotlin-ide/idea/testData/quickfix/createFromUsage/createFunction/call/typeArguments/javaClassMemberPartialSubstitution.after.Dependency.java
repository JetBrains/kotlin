import org.jetbrains.annotations.NotNull;

class B<T>{
    final T t;

    public <U> U foo(U u, @NotNull String s) {
        return null;
    }
}