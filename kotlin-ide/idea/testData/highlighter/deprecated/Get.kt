class MyClass {}

@Deprecated("Use A instead") operator fun MyClass.get(i: MyClass): MyClass { return i }

fun test() {
  val x1 = MyClass()
  val x2 = MyClass()

  <warning descr="[DEPRECATION] 'get(MyClass): MyClass' is deprecated. Use A instead">x1[x2]</warning>
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS
