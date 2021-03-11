// OUT_OF_CODE_BLOCK: FALSE
// ERROR: Unresolved reference: a
class LocalFunWithBodyInClass {
  fun test() {
    fun hello() {
      <caret>
    }
  }
}