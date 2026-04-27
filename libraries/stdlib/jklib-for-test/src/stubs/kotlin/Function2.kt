package kotlin

public interface Function2<in P1, in P2, out R> : Function<R> {
    public operator fun invoke(p1: P1, p2: P2): R
}
