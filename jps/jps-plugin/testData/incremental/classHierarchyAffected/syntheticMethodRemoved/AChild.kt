package foo

class AChild : A {
  override fun <T> createB(items: Collection<T>, renderer: (T) -> String): B {
    return B()
  }

  override fun <T> createB(items: Collection<T>, renderer: B.Presentation.(T) -> Unit): B {
    return B()
  }

}