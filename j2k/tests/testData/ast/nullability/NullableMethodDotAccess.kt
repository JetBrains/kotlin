class C {
    fun getString(b: Boolean): String? {
        return if (b) "a" else null
    }

    fun foo(): Int {
        return getString(true)!!.length()
    }
}
