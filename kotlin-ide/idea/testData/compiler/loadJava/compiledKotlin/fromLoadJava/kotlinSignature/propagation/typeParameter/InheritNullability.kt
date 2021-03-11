package test

public interface InheritNullability {

    public interface Super {
        public fun <A: CharSequence> foo(a: A)
    }

    public interface Sub: Super {
        override fun <B: CharSequence> foo(a: B)
    }
}
