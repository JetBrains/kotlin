package test

public interface DeeplySubstitutedClassParameter {

    public interface Super<T> {
        public fun foo(): T

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Middle<E>: Super<E> {
        override fun foo(): E
    }

    public interface Sub: Middle<String> {
        override fun foo(): String
    }
}
