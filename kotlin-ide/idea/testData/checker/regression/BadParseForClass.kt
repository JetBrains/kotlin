// FIR_COMPARISON

fun main(<warning descr="[UNUSED_PARAMETER] Parameter 'args' is never used">args</warning>: Array<String>) {
    String.class<EOLError descr="Name expected"></EOLError>
}

// EA-56152: An attempt to build light class in checker to get diagnotics