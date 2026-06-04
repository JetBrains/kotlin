fun clamp(x: Int): Int {
    if (x < 0) return 0
    if (x > 10) return 10
    return x
}
fun main() {
    var r: Int

    r = clamp(-3); if (r != 0)  error("clamp(-3) = $r, expected 0")
    r = clamp(5);  if (r != 5)  error("clamp(5) = $r, expected 5")
    r = clamp(99); if (r != 10) error("clamp(99) = $r, expected 10")
}
