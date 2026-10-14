package foo

interface A {

  @Deprecated("Use another createB method", level = DeprecationLevel.HIDDEN)
  fun <T> createB(items: Collection<T>, renderer: (T) -> String): B
    
  fun <T> createB(items: Collection<T>, renderer: B.Presentation.(T) -> Unit): B
}