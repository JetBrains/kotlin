// OPTION: 3
fun foo(n : Int): Int {
    return 10 +
          <caret>if (n > 0) {
              1
          } else {
              -1
          }
}
