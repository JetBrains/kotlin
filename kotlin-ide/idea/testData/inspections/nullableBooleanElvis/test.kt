fun foo() {
    var a: Boolean? = null
    var b: Boolean? = null
    if (a ?: false) {

    }
    if (!(a ?: false)) {

    }
    if (a ?: false || !(b ?: true)) {

    }
    val x = a ?: false    // INFORMATION -- not reported in batch
    val y = !(b ?: true)  // INFORMATION -- not reported in batch
}