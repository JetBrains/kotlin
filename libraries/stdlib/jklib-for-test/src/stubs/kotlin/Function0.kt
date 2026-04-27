package kotlin

public interface Function0<out R> : Function<R> {
    public operator fun invoke(): R
}
