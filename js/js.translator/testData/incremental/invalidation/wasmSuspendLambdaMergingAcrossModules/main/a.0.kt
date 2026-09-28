import sample.*

// Structurally identical to the lambda in lib1, so both modules get the same shared suspend lambda
// class and are expected to share a single definition after linking.
fun fromMain(x: Any): suspend () -> Any = { id(x); x }
