package test

public interface TwoSuperclassesReturnSameJavaType {

    public interface Super1 {
        public fun foo(): CharSequence?

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Super2 {
        public fun foo(): CharSequence

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super1, Super2 {
        override fun foo(): CharSequence
    }
}
