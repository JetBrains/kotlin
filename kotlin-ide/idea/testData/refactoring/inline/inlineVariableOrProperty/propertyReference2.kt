class X {
    private fun f(first: String?, second: String?): Int? {
        val predicate = ::ref
        return when {
            predi<caret>cate(first) -> calc(first!!)
            predicate(second) -> calc(second!!)
            else -> null
        }
    }

    private fun ref(value: String?) = value != null
    private fun calc(value: String) = value.toInt()
}