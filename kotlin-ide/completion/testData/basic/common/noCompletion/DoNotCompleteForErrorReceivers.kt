// FIR_COMPARISON
/// KT-1187 Wrong unnecessary completion

fun anyfun() {
    a.b.c.d.e.f.<caret>
}

// INVOCATION_COUNT: 1
// NUMBER: 0