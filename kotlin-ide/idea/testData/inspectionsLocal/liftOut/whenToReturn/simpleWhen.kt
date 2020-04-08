fun test(n: Int): String {
    <caret>when (n) {
      1 -> return "one"
      else -> return "two"
    }
}