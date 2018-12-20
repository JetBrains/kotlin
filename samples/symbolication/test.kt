fun f(p: Int) {
  if (p == 0) throw Error()
  f(p - 1)
}

fun g1(p: Int) {
  if (p == 0) throw Error()
  g2(p - 1)
}

fun g2(p: Int) {
  g1(p - 1)
}

fun main() {
  try {
    f(10)
  } catch (e: Throwable) {
    e.printStackTrace()
  }
  g1(10)
}
