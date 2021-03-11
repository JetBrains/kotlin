//ALLOW_AST_ACCESS
package test

public interface InheritProjectionKind {

    public interface Super {
        public fun foo(p: MutableList<in String>)

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(p: MutableList<in String>)
    }
}
