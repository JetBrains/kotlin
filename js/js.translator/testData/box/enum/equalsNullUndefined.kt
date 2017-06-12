// EXPECTED_REACHABLE_NODES: 529
enum class A {
    X,
    Y
}

fun getNull(): A? = null

fun getUndefined(): A? = js("void 0")

fun getX(): A? = A.X

fun <T : A?> testGenericEquals(a: T, b: T) = a == b

fun <T : A?> testGenericEqualsToNull(a: T) = a == null

fun <T : A> testGenericEqualsNotNull(a: T, b: T) = a == b

fun box(): String {
    if (getX() == getNull()) return "fail1"
    if (getX() == getUndefined()) return "fail2"
    if (getNull() != null) return "fail3"
    if (getUndefined() != null) return "fail4"
    if (getUndefined() != getNull()) return "fail5"
    if (A.X == getUndefined()) return "fail6"
    if (A.X == getNull()) return "fail6"
    if (A.X == null) return "fail7"
    if (A.X == A.Y) return "fail8"
    if (A.X != getX()) return "fail9"
    if (!testGenericEquals(getUndefined(), getNull())) return "fail10"
    if (testGenericEqualsNotNull(A.X, A.Y)) return "fail11"
    if (!testGenericEqualsNotNull(A.X, getX()!!)) return "fail12"
    if (!testGenericEqualsToNull(getNull())) return "fail13"
    if (!testGenericEqualsToNull(getUndefined())) return "fail14"
    if (testGenericEqualsToNull(getX())) return "fail15"

    return "OK"
}

