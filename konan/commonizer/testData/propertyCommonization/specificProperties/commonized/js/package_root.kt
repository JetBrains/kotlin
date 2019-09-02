const val constProperty1 = 42
const val constProperty2 = 42

actual val delegatedProperty1: Int by lazy { 42 }
actual val delegatedProperty2: Int by lazy { 42 }
actual val delegatedProperty3: Int by mapOf("delegatedProperty3" to 42)
actual val delegatedProperty4: Int by mapOf("delegatedProperty4" to 42)

lateinit var lateinitProperty1: String
lateinit var lateinitProperty2: String

actual inline val inlineProperty1 get() = 42
actual inline val inlineProperty2 get() = 42
