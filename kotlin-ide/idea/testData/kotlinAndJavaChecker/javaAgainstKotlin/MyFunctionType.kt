package test

public interface MyFunction<in P1, out R> : Function<R> {
    public operator fun invoke(p1: P1): R
}


