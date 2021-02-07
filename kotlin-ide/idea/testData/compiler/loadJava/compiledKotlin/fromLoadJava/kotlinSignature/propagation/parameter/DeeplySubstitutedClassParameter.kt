package test

public interface DeeplySubstitutedClassParameter {

    public interface Super<T> {
        public fun foo(t: T)

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Middle<E>: Super<E> {
        override fun foo(t: E)
    }

    public interface Sub: Middle<String> {
        override fun foo(t: String)
    }
}
