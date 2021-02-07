package test

public interface UseParameterAsUpperBound {

    public interface Super {
        public fun <A, B: A> foo(a: A, b: B)
    }

    public interface Sub: Super {
        override fun <B, A: B> foo(a: B, b: A)
    }
}
