//ALLOW_AST_ACCESS
package test

public interface TwoSuperclassesConflictingProjectionKinds {

    public interface Super1 {
        public fun foo(): MutableCollection<CharSequence>

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Super2 {
        public fun foo(): MutableCollection<out CharSequence>

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super1, Super2 {
        override fun foo(): MutableCollection<CharSequence>
    }
}
