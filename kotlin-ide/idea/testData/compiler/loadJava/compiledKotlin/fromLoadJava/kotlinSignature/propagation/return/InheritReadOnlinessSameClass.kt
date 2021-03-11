package test

public interface InheritReadOnlinessSameClass {

    public interface Super {
        public fun foo(): List<String>

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(): List<String>
    }
}
