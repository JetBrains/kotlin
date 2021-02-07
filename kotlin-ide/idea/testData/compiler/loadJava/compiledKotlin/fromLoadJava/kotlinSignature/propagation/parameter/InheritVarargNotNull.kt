//ALLOW_AST_ACCESS
package test

public interface InheritVarargNotNull {

    public interface Super {
        public fun foo(vararg p: String)

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(vararg p: String)
    }
}
