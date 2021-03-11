// PARAM_TYPES: kotlin.String?, kotlin.Comparable<kotlin.String>?, kotlin.CharSequence?, java.io.Serializable?, kotlin.Any?
// PARAM_DESCRIPTOR: val s: kotlin.String? defined in foo

// SIBLING:
fun foo(): Int {
    val s: String? = ""
    return if (true) {
        <selection>s!!.length</selection>
    } else {
        s!!.length
    }
}