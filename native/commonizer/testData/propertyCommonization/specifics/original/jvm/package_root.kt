val delegatedProperty1: Int by lazy { 42 }
val delegatedProperty2 = 42 // intentionally left as non-delegated
val delegatedProperty3: Int by mapOf("delegatedProperty3" to 42)
val delegatedProperty4 = 42 // intentionally left as non-delegated

lateinit var lateinitProperty1: String
var lateinitProperty2 = "hello" // intentionally left as non-lateinit

inline val inlineProperty1 get() = 42
val inlineProperty2 inline get() = 42
val inlineProperty3 = 42 // intentionally left as non-inline

inline var inlineProperty4
    get() = 42
    set(value) = Unit
var inlineProperty5
    inline get() = 42
    inline set(value) = Unit
var inlineProperty6
    inline get() = 42
    set(value) = Unit
var inlineProperty7
    get() = 42
    inline set(value) = Unit
var inlineProperty8
    get() = 42
    set(value) = Unit

external val externalProperty1: Int
val externalProperty2: Int = 1

var externalSetGet: Int
    external get
    external set