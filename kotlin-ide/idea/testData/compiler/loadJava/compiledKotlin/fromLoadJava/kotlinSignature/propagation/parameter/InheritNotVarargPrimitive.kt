package test

public interface InheritNotVarargPrimitive {

    public interface Super {
        public fun foo(p0: IntArray?)

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(p0: IntArray?)
    }
}
