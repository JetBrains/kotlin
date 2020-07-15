// MODE: return
fun foo() {
    run {
        val length: Int? = null
        length ?: 0<# ^run #>
    }
}