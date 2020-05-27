actual val delegatedProperty1: Int by lazy { 42 }
actual val delegatedProperty2 = 42 // intentionally left as non-delegated
actual val delegatedProperty3: Int by mapOf("delegatedProperty3" to 42)
actual val delegatedProperty4 = 42 // intentionally left as non-delegated

lateinit var lateinitProperty1: String
var lateinitProperty2 = "hello" // intentionally left as non-lateinit

actual val inlineProperty1 inline get() = 42
actual inline val inlineProperty2 get() = 42
actual val inlineProperty3 = 42 // intentionally left as non-inline

actual var inlineProperty4
    inline get() = 42
    inline set(value) = Unit
inline actual var inlineProperty5
    get() = 42
    set(value) = Unit
actual var inlineProperty6
    inline get() = 42
    set(value) = Unit
actual var inlineProperty7
    get() = 42
    inline set(value) = Unit
actual var inlineProperty8
    get() = 42
    set(value) = Unit

actual external val externalProperty1: Int
actual val externalProperty2: Int = 1
