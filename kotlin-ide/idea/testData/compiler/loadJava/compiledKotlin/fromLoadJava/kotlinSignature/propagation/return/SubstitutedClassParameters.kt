package test

public interface SubstitutedClassParameters {

    public interface Super1<T> {
        public fun foo(): T

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Super2<E> {
        public fun foo(): E

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super1<String>, Super2<String> {
        override fun foo(): String
    }
}
