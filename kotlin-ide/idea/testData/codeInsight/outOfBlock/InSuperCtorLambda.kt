// OUT_OF_CODE_BLOCK: FALSE
// TYPE: 1
// ERROR: Unresolved reference: i1
open class Base(init: () -> Unit)

class Some(i: Int) : Base({
  val t = i<caret>
})