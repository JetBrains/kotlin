package kotlin

public interface Function5<in P1, in P2, in P3, in P4, in P5, out R> : Function<R> {
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5): R
}
