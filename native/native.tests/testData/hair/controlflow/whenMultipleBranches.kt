fun classify(x: Int): Int = when {
    x < 0 -> -1
    x > 0 -> 1
    else -> 0
}
fun main() {
    var r: Int

    r = classify(-5); if (r != -1) error("classify(-5) = $r, expected -1")
    r = classify(0);  if (r != 0)  error("classify(0) = $r, expected 0")
    r = classify(7);  if (r != 1)  error("classify(7) = $r, expected 1")
}
