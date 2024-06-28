@JvmInline
value class A(private val i: Int?)

@JvmInline
value class B(private val f: suspend () -> Unit)

@JvmInline
value class Z(val s: String)
