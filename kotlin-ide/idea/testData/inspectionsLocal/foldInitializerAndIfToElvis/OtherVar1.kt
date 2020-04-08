// PROBLEM: none
class C {
    var v: String? = null

    fun foo(p: List<String?>): Int {
        val v = p[0]
        <caret>if (this.v == null) return -1
        return v!!.length
    }
}

