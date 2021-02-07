package test

public interface InheritReadOnliness {

    public interface Super {
        public fun foo(p: List<String>)

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(p: List<String>)
    }
}
