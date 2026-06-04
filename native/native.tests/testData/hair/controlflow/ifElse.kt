fun pick(cond: Boolean): Int = if (cond) 1 else 2
fun main() {
    var r: Int

    r = pick(true);  if (r != 1) error("pick(true) = $r, expected 1")
    r = pick(false); if (r != 2) error("pick(false) = $r, expected 2")
}
