//ALLOW_AST_ACCESS
package test

public interface InheritNotVararg {

    public interface Super {
        public fun foo(p0: Array<out String>?)

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(p0: Array<out String>?)
    }
}
