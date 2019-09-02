const val constProperty1 = 42
val constProperty2 = 42 // intentionally left as non-const

actual val delegatedProperty1: Int by lazy { 42 }
actual val delegatedProperty2 = 42 // intentionally left as non-delegated
actual val delegatedProperty3: Int by mapOf("delegatedProperty3" to 42)
actual val delegatedProperty4 = 42 // intentionally left as non-delegated

lateinit var lateinitProperty1: String
var lateinitProperty2 = "hello" // intentionally left as non-lateinit

actual inline val inlineProperty1 get() = 42
actual val inlineProperty2 = 42 // intentionally left as non-inline
