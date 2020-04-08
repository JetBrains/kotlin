// "Add constructor parameters from Base(Int, Int)" "true"
open class Base(p1: Int, private val p2: Int = 0)

class C(p: Int) : Base<caret>
