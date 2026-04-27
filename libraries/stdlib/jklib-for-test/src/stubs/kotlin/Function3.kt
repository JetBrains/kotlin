package kotlin

public interface Function3<in P1, in P2, in P3, out R> : Function<R> {
    public operator fun invoke(p1: P1, p2: P2, p3: P3): R
}
