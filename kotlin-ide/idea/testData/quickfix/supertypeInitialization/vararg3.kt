// "Add constructor parameters from Base(Int, IntArray)" "true"
open class Base(p1: Int, vararg p2: Int)

class C(vararg p2: Int) : Base<caret>