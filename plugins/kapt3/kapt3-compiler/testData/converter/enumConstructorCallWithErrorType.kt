// CORRECT_ERROR_TYPES

interface I

@Suppress("UNRESOLVED_REFERENCE")
enum class E(val i: I) {
    E1(Unresolved1),
    E2(Unresolved2),
    E3(Unresolved3),
}
