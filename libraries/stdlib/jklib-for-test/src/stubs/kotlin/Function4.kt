package kotlin

public interface Function4<in P1, in P2, in P3, in P4, out R> : Function<R> {
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4): R
}
