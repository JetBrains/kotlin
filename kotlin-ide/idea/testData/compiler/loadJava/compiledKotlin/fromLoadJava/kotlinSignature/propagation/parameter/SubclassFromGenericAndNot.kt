package test

// Extracted from KT-3302, see Kt3302 test, as well
public interface SubclassFromGenericAndNot {

    public interface NonGeneric  {
        public fun foo(p0: String)

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Generic<T>  {
        public fun foo(key: T)

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub : NonGeneric, Generic<String> {
        override fun foo(key: String)
    }
}
