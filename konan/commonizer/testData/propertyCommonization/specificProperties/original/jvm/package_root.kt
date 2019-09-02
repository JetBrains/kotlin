const val constProperty1 = 42
val constProperty2 = 42 // intentionally left as non-const

val delegatedProperty1: Int by lazy { 42 }
val delegatedProperty2 = 42 // intentionally left as non-delegated
val delegatedProperty3: Int by mapOf("delegatedProperty3" to 42)
val delegatedProperty4 = 42 // intentionally left as non-delegated

lateinit var lateinitProperty1: String
var lateinitProperty2 = "hello" // intentionally left as non-lateinit

inline val inlineProperty1 get() = 42
val inlineProperty2 = 42 // intentionally left as non-inline
