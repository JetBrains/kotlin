// PARAM_TYPES: X<kotlin.Any?>
// PARAM_TYPES: kotlin.String?, kotlin.Comparable<kotlin.String>?, kotlin.CharSequence?, java.io.Serializable?, kotlin.Any?
// PARAM_DESCRIPTOR: value-parameter x: X<kotlin.Any?> defined in foo
// PARAM_DESCRIPTOR: value-parameter s: kotlin.String? defined in foo
class X<T> {
    fun add(t: T) {

    }
}

// SIBLING:
fun foo(s: String?, x: X<Any?>) {
    when {
        s != null -> <selection>x.add(s)</selection>
    }
}