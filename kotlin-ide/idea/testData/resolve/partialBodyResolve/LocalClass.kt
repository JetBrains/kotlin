fun foo(p: String?) {
    class LocalClass {
        fun f(): String? {
            if (p == null) return null
            print(p.size)
            return ""
        }
    }

    <caret>p?.size
}