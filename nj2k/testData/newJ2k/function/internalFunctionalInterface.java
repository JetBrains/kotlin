interface FunctionalI<A, B> {
    B apply(A x);
}

class Test {
    <A, B> B foo(A value, FunctionalI<A, B> fun) {
        return fun.apply(value);
    }

    Double toDouble(Integer x) {
        return x.doubleValue();
    }

    public double nya() {
        return foo(1, this::toDouble);
    }
}