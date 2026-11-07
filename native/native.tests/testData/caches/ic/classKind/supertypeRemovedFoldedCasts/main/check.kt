// A folded safe upcast (SAFE_CAST erased as no-cast-needed).
fun safeUp(x: S): Boolean = (x as? Marker) != null

// A folded hard upcast (CAST erased as no-cast-needed).
fun hardUp(x: S): String = try {
    x as Marker
    "cast-ok"
} catch (e: ClassCastException) {
    "cce"
}
