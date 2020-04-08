class C(val s: String?) {
    fun foo(p: Boolean) {
        if (p) {
            print(s!!)
        }
        else {
            print(this.s!!)
        }

        <caret>s.length()
    }
}
