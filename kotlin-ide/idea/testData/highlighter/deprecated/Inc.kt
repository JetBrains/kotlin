class MyClass {
  val i = 0
}

@Deprecated("Use A instead") operator fun MyClass.inc(): MyClass { return MyClass() }

fun test() {
    var x3 = MyClass()
    x3<warning descr="[DEPRECATION] 'inc(): MyClass' is deprecated. Use A instead">++</warning>
    x3.i
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS
