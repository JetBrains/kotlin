//ALLOW_AST_ACCESS
package test

public interface InheritProjectionKind {

    public interface Super {
        public fun foo(): MutableCollection<out Number>

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(): MutableList<out Number>
    }
}
