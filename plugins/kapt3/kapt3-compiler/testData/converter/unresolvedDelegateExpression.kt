// CORRECT_ERROR_TYPES
// NO_VALIDATION

open class C

@Suppress("UNRESOLVED_REFERENCE")
class B : NonExisting {
    @Suppress("UNRESOLVED_REFERENCE_WRONG_RECEIVER")
    val a: String by flaf()
}

interface I

@Suppress("UNRESOLVED_REFERENCE")
class D : I by NonExisting

fun C.flaf() = "OK"
