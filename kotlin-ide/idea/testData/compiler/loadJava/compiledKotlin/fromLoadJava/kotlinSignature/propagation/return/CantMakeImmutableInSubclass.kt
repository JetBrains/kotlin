package test

public interface CantMakeImmutableInSubclass {

    public interface Super {
        public fun foo(): MutableCollection<String>

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun foo(): MutableList<String>
    }
}
