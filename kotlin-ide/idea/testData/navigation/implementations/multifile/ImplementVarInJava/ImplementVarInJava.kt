package testing.kt

open class KotlinBase {
  open var <caret>some = "Test"
}

// REF: (in testing.jj.JavaBase).getSome()
// REF: (in testing.jj.JavaBase).setSome(String)