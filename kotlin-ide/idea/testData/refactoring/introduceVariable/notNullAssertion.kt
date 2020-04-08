fun foo(): Int {
    val s: String? = ""
    return if (true) {
        <selection>s!!.length</selection>
    } else {
        s!!.length
    }
}