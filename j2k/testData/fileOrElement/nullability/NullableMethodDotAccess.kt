internal class C {
    internal fun getString(b: Boolean): String? {
        return if (b) "a" else null
    }

    internal fun foo(): Int {
        return getString(true)!!.length()
    }
}
