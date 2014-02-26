package foo

fun box(): String {

  fun Int.foo(): Boolean {
      fun Int.bar() = this == 2 && this@foo == 1
      val b = { this == 1 }

      return this == 1 && 2.bar() && b()
  }

  if (!1.foo()) return "Failed"

  return "OK"
}
