class Foo() {
    val s = this@F<selection>o</selection>o.toString()
}

// KT-4515: Extract variable can attempt to extract a label from a labelled statement.
/* this@Foo */
