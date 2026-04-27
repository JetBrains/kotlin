package kotlin

public interface Function8<in P1, in P2, in P3, in P4, in P5, in P6, in P7, in P8, out R> : Function<R> {
    public operator fun invoke(p1: P1, p2: P2, p3: P3, p4: P4, p5: P5, p6: P6, p7: P7, p8: P8): R
}
