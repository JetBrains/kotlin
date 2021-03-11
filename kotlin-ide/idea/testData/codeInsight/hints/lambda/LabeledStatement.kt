// MODE: return
fun test() {
    run {
        val files: Any? = null
        run@
        12<# ^run #>
    }

    run {
        val files: Any? = null
        run@12<# ^run #>
    }
}