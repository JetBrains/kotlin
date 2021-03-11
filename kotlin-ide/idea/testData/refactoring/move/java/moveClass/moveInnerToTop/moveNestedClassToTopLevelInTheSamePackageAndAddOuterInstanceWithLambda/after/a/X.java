package a;

import kotlin.jvm.functions.Function0;

public class X {
    private final A outer;

    public X(A outer, Function0<String> f) {
        this.outer = outer;
        System.out.println(f.invoke());
    }
}
