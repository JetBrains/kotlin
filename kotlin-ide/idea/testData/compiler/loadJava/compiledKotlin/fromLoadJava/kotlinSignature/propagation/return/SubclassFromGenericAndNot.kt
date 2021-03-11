package test

public interface SubclassFromGenericAndNot {

    public interface NonGeneric  {
        public fun foo(): String?

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Generic<T>  {
        public fun foo(): T

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub : NonGeneric, Generic<String> {
        override fun foo(): String
    }
}
