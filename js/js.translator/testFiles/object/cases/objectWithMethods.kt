package foo

class Test {
  private val a = object {
      fun c() = 3
      fun b() = 2
  }

  fun doTest(): Boolean {
    if (a.c() != 3) {
        return false;
    }
    if (a.b() != 2) {
        return false;
    }
    return true;
  }
}

fun box(): Boolean {
    return Test().doTest();
}
