package test

public interface SubstitutedClassParameters {

    public interface Super1<T> {
        public fun foo(t: T)

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Super2<E> {
        public fun foo(t: E)

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super1<String>, Super2<String> {
        override fun foo(t: String)
    }
}
