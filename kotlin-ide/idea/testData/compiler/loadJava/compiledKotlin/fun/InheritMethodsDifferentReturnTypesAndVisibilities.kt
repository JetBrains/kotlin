package test

public interface Super1 {
    public fun foo(): CharSequence
    private fun bar(): String = ""
}

public interface Super2 {
    private fun foo(): String = ""
    public fun bar(): CharSequence
}

public interface Sub: Super1, Super2 {
}
