package foo;

val Double.abs: Double
    get() = if (this > 0) this else -this

fun box(): Boolean {
    if (4.0.abs != 4.0) return false;
    if ((-5.2).abs != 5.2) return false;

    return true;
}
