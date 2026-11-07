// P is private-in-file: CacheInfoBuilder still writes its per-file class_fields entry, but
// under a FileLocalSignature whose container FileSignature is compared by identity and can
// never be matched again by a consumer module.
private open class P {
    fun p(): String = "ok"
}

// The object literal must live in a private inline function: a non-private inline function
// may not access a private-in-file type.
private inline fun probe(): String = object : P() {}.p()

// Inlined into the friend module main, which re-creates the object literal as a local class
// and must compute its field layout over the private superclass P.
internal inline fun f(): String = probe()
