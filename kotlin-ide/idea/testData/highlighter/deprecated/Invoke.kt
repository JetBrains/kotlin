class MyRunnable() {}

@Deprecated("Use A instead") operator fun MyRunnable.invoke() {
}

fun test() {
  val m = MyRunnable()
  <warning descr="[DEPRECATION] 'invoke(): Unit' is deprecated. Use A instead">m</warning>()
}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS
