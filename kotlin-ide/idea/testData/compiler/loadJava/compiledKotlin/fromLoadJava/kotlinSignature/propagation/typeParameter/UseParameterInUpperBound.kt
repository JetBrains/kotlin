package test

public interface UseParameterInUpperBound {

    public interface Super {
        public fun <A, B: List<A>> foo(a: A, b: B)
    }

    public interface Sub: Super {
        override fun <B, A: List<B>> foo(a: B, b: A)
    }
}
