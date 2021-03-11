package test

public interface InheritReadOnliness {

    public interface Super {
        public fun <A: List<String>> foo(a: A)
    }

    public interface Sub: Super {
        override fun <B: List<String>> foo(a: B)
    }
}
